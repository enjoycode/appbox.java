package appbox.design.idea;

import appbox.logging.Log;
import com.intellij.codeInsight.completion.JavaCompletionContributor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.reference.SoftReference;

public final class CompletionUtil {
    private static final Key<SoftReference<Pair<PsiFile, Document>>> FILE_COPY_KEY = Key.create("CompletionFileCopy");

    public static final  String DUMMY_IDENTIFIER         = "IntellijIdeaRulezzz ";
    private static final String DUMMY_IDENTIFIER_TRIMMED = "IntellijIdeaRulezzz";

    public static String customizeDummyIdentifier(PsiFile file, int offset) {
        //only for CompletionType.BASIC
        if (PsiTreeUtil.findElementOfClassAtOffset(file, offset - 1,
                PsiReferenceParameterList.class, false) != null) {
            return DUMMY_IDENTIFIER_TRIMMED;
        }

        if (JavaCompletionContributor.semicolonNeeded(file, offset)) {
            return DUMMY_IDENTIFIER_TRIMMED + ";";
        }

        PsiElement leaf = file.findElementAt(offset);
        if (leaf instanceof PsiIdentifier || leaf instanceof PsiKeyword) {
            return DUMMY_IDENTIFIER_TRIMMED;
        }

        return null;
    }

    public static PsiFile obtainFileCopy(PsiFile file) {
        final VirtualFile virtualFile = file.getVirtualFile();
        boolean mayCacheCopy = file.isPhysical() &&
                // we don't want to cache code fragment copies even if they appear to be physical
                virtualFile != null && virtualFile.isInLocalFileSystem();
        if (mayCacheCopy) {
            final Pair<PsiFile, Document> cached = SoftReference.dereference(file.getUserData(FILE_COPY_KEY));
            if (cached != null && isCopyUpToDate(cached.second, cached.first, file)) {
                PsiFile copy = cached.first;
                //CompletionAssertions.assertCorrectOriginalFile("Cached", file, copy);
                return copy;
            }
        }

        final PsiFile copy = (PsiFile) file.copy();
        if (copy.isPhysical() || copy.getViewProvider().isEventSystemEnabled()) {
            Log.error("File copy should be non-physical and non-event-system-enabled! Language=" +
                    file.getLanguage() +
                    "; file=" +
                    file +
                    " of " +
                    file.getClass());
        }
        //CompletionAssertions.assertCorrectOriginalFile("New", file, copy);

        if (mayCacheCopy) {
            final Document document = copy.getViewProvider().getDocument();
            assert document != null;
            syncAcceptSlashR(file.getViewProvider().getDocument(), document);
            file.putUserData(FILE_COPY_KEY, new SoftReference<>(Pair.create(copy, document)));
        }
        return copy;
    }

    private static boolean isCopyUpToDate(Document document, PsiFile copyFile, PsiFile originalFile) {
        if (!copyFile.getClass().equals(originalFile.getClass()) ||
                !copyFile.isValid() ||
                !copyFile.getName().equals(originalFile.getName())) {
            return false;
        }
        // the psi file cache might have been cleared by some external activity,
        // in which case PSI-document sync may stop working
        PsiFile current = PsiDocumentManager.getInstance(copyFile.getProject()).getPsiFile(document);
        return current != null && current.getViewProvider().getPsi(copyFile.getLanguage()) == copyFile;
    }

    private static void syncAcceptSlashR(Document originalDocument, Document documentCopy) {
        if (!(originalDocument instanceof DocumentImpl) || !(documentCopy instanceof DocumentImpl)) {
            return;
        }

        ((DocumentImpl)documentCopy).setAcceptSlashR(((DocumentImpl)originalDocument).acceptsSlashR());
    }

}
