package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.idea.*;
import appbox.design.idea.CompletionUtil;
import appbox.logging.Log;
import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.BlockSupportImpl;
import com.intellij.psi.impl.ChangedPsiRangeUtil;
import com.intellij.psi.impl.source.tree.FileElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 一个TypeSystem对应一个Workspace实例，管理JavaProject及相应的虚拟文件
 */
public final class Workspace {
    private final IdeaProjectEnvironment          projectEnvironment;
    private final HashMap<Long, ModelVirtualFile> openedFiles           = new HashMap<>();
    public final  ModelVirtualFileSystem          virtualFileSystem;
    private final JavaCompletionContributor       completionContributor = new JavaCompletionContributor();

    public Workspace(DesignHub designHub) {
        projectEnvironment = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);
        virtualFileSystem  = new ModelVirtualFileSystem(designHub);
    }

    public Document openDocument(long modelId) {
        //TODO:仅允许打开特定类型的
        var file = virtualFileSystem.findFileByModelId(modelId);
        openedFiles.put(modelId, file);
        return FileDocumentManager.getInstance().getDocument(file);
    }

    public Document findOpenedDocument(long modelId) {
        var file = openedFiles.get(modelId);
        if (file == null) {
            return null;
        }

        return FileDocumentManager.getInstance().getDocument(file);
    }

    public void changeDocument(Document doc, int startLine, int startColumn,
                               int endLine, int endColumn, CharSequence newText) {
        int startOffset   = doc.getLineStartOffset(startLine) + startColumn;
        int endOffset     = doc.getLineStartOffset(endLine) + endColumn;
        var lastCommitted = doc.getImmutableCharSequence(); //PsiDocumentManager.getLastCommittedText(doc)

        WriteCommandAction.runWriteCommandAction(projectEnvironment.getProject(), () -> {
            doc.replaceString(startOffset, endOffset, newText);
        });

        //incremental reparse
        var psiFile = PsiDocumentManager.getInstance(projectEnvironment.getProject()).getPsiFile(doc);
        var changedRange = getChangedPsiRange(psiFile, doc,
                lastCommitted, doc.getImmutableCharSequence(), startOffset, newText.length());
        //changedRange = ChangedPsiRangeUtil.getChangedPsiRange(psiFile, (FileElement) psiFile.getNode(), doc.getImmutableCharSequence());
        if (changedRange == null) {
            changedRange = ProperTextRange.create(0, lastCommitted.length());
        }
        var bs                = BlockSupportImpl.getInstance(projectEnvironment.getProject());
        var progressIndicator = new EmptyProgressIndicator(); //TODO: 单例?
        var diffLog = bs.reparseRange(psiFile, psiFile.getNode(), changedRange, doc.getImmutableCharSequence(),
                progressIndicator, lastCommitted);
        //diffLog.doActualPsiChange(psiFile);
        diffLog.performActualPsiChange(psiFile);
    }

    private static ProperTextRange getChangedPsiRange(PsiFile file, Document document,
                                                      CharSequence oldDocumentText,
                                                      CharSequence newDocumentText,
                                                      int eventOffset, int eventOldLength) {
        int psiLength = oldDocumentText.length();
        int prefix    = eventOffset;
        int suffix    = psiLength - eventOffset - eventOldLength;
        //lengthBeforeEvent = lengthBeforeEvent - eventOldLength + eventNewLength;

        if ((prefix == psiLength || suffix == psiLength) && newDocumentText.length() == psiLength) {
            return null;
        }
        ////Important! delete+insert sequence can give some of same chars back, lets grow affixes to include them.
        //int shortestLength = Math.min(psiLength, newDocumentText.length());
        //while (prefix < shortestLength && oldDocumentText.charAt(prefix) == newDocumentText.charAt(prefix)) {
        //    prefix++;
        //}
        //while (suffix < shortestLength - prefix &&
        //        oldDocumentText.charAt(psiLength - suffix - 1) == newDocumentText.charAt(newDocumentText.length() - suffix - 1)) {
        //    suffix++;
        //}
        int end = Math.max(prefix, psiLength - suffix);
        if (end == prefix && newDocumentText.length() == oldDocumentText.length()) return null;
        return ProperTextRange.create(prefix, end);
    }

    public List<CompletionResult> fillCompletion(Document doc, int line, int column, String wordToComplete) {
        var list = new ArrayList<CompletionResult>();

        var psiFile = PsiDocumentManager.getInstance(projectEnvironment.getProject()).getPsiFile(doc);
        //var cursor  = doc.getLineStartOffset(line) + column - 1; //暂减1
        var cursor = doc.getLineStartOffset(line) + column;
        //Log.debug(doc.getText(new TextRange(doc.getLineStartOffset(line), cursor)));

        //before completion
        var dummyIdentifier = CompletionUtil.customizeDummyIdentifier(psiFile, cursor);
        if (dummyIdentifier == null) {
            dummyIdentifier = CompletionUtil.DUMMY_IDENTIFIER; //TODO:
        }
        changeDocument(doc, line, column, line, column, dummyIdentifier);
        //Log.debug(doc.getText());

        //do completion
        try {
            var position = psiFile.findElementAt(cursor);
            Log.debug(String.format("Completion at: %d %s %s", cursor, position.getClass().getSimpleName(), position.getText()));
            //do completion
            var cParameters = new CompletionParameters(position, psiFile, CompletionType.BASIC,
                    cursor, 0, new IdeaEditor() /*TODO*/, () -> false);
            var prefixMatcher = PrefixMatcher.ALWAYS_TRUE;
            if (wordToComplete != null) {
                prefixMatcher = new PlainPrefixMatcher(wordToComplete);
            }
            var cResultSet = new IdeaCompletionResultSet(
                    list::add, prefixMatcher, completionContributor, cParameters, null, null);

            completionContributor.fillCompletionVariants(cParameters, cResultSet);
        } finally {
            //after completion
            changeDocument(doc, line, column, line, column + dummyIdentifier.length(), "");
            //Log.debug(doc.getText());
        }

        return list;
    }

}
