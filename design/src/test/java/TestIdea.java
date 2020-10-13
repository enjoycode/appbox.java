import appbox.design.idea.IdeaApplicationEnvironment;
import appbox.design.idea.IdeaProjectEnvironment;
import appbox.design.idea.IdeaPsiDocumentManager;
import appbox.model.ServiceModel;
import com.intellij.codeInsight.completion.*;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.MockDocumentEvent;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.text.BlockSupport;
import com.intellij.util.DocumentUtil;
import com.intellij.util.FileContentUtil;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.SmartList;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import static org.junit.jupiter.api.Assertions.*;

public class TestIdea {

    @Test
    public void testIdea() {
        //LanguageParserDefinitions.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, TestFileViewProvider.parserDefinition);
        var temp = LanguageLevel.HIGHEST; //new CoreLanguageLevelProjectExtension();

        //var app = new ApplicationImpl(true, false, true, true);
        var app = new TestApplication();

        var project = new TestProject();

        var module = new TestModule(project, "Module1");

        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java",
                "class A {}", System.currentTimeMillis());
        root.addChild(file1);
        project.fileManager.addToClasspath(root);

        //测试VirtualFile -> PsiFile
        var fileViewProvider = new TestFileViewProvider(project.psiManager, file1);
        var psiFile          = fileViewProvider.getPsi(JavaLanguage.INSTANCE);
        var childs           = psiFile.getChildren();

        //FileViewProviderFactory factory = LanguageFileViewProviders.INSTANCE.forLanguage(JavaLanguage.INSTANCE);
        //var vfp = new SingleRootFileViewProvider(project.psiManager, file1);


        //var psiFile = project.psiManager.findFile(file1);

        var cs = project.fileManager.findClass("A", GlobalSearchScope.projectScope(project));

        //var psiManager = new PsiManagerImpl(project);

        //PsiJavaParserFacadeImpl psiParserFacade = new PsiJavaParserFacadeImpl(project);
        //JavaPsiFacadeImpl psiFacade  = new JavaPsiFacadeImpl(project);
        //PsiManager        psiManager = PsiManager.getInstance(project);
        //ModuleManager.getInstance(project).newModule(moduleFile, EmptyModuleType.EMPTY_MODULE);

        //var m = psiFacade.findModule("TestModule",
        //        GlobalSearchScope.EMPTY_SCOPE
        //        /*GlobalSearchScope.allScope(project)*/);

    }

    @Test
    public void testIdea2() {
        var lastDisposable = Disposer.newDisposable();
        var app            = new IdeaApplicationEnvironment(lastDisposable);
        var prj            = new IdeaProjectEnvironment(lastDisposable, app);

        prj.addJarToClassPath(new File("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar"));
        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java",
                "package s.impl; class A {A b; void say(){b.toString();}}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psiFile = PsiManager.getInstance(prj.getProject()).findFile(file1);
        //var ref     = psiFile.findReferenceAt(43); //25, 41
        //var res     = ref.resolve();
        //var res = ((PsiJavaCodeReferenceElement) ref).advancedResolve(true);
        //var vars = ref.getVariants(); //代码必须设package

        //for (int i = 0; i < 10; i++) {
        var cusor       = 43;
        var position    = psiFile.findElementAt(cusor);
        var contributor = new JavaCompletionContributor();

        var cParameters = new CompletionParameters(position, psiFile, CompletionType.BASIC,
                cusor, 0, new TestEditor(), new CompletionProcess() {
            @Override
            public boolean isAutopopupCompletion() {
                return false;
            }
        });

        //测试直接使用JavaCompletionContributor
        var cResultSet = new TestCompletionResultSet(
                completionResult -> System.out.println(completionResult),
                PrefixMatcher.ALWAYS_TRUE, contributor, cParameters, null, null);

        contributor.fillCompletionVariants(cParameters, cResultSet);
        //}
    }

    @Test
    public void testReparse() { //测试修改代码后incremental reparse
        var lastDisposable = Disposer.newDisposable();
        var app            = new IdeaApplicationEnvironment(lastDisposable, false);
        var prj            = new IdeaProjectEnvironment(lastDisposable, app);
        var vf = new TestVirtualFile("A.java", "class A {\n void say(){\n\n}\n}\n",
                System.currentTimeMillis());
        var psiDocumentManager =
                (IdeaPsiDocumentManager) PsiDocumentManager.getInstance(prj.getProject());

        var psiFile1 = PsiManager.getInstance(prj.getProject()).findFile(vf);
        var class1   = ((PsiJavaFileImpl) psiFile1).getClasses();
        //assertTrue(psiFile1.getViewProvider().supportsIncrementalReparse(JavaLanguage.INSTANCE));

        //测试改变文件内容
        var doc           = FileDocumentManager.getInstance().getDocument(vf);
        var lastCommitted = psiDocumentManager.getLastCommittedText(doc);
        WriteCommandAction.runWriteCommandAction(prj.getProject(), () -> {
            //doc.insertString(6, "B"); //full reparse
            //doc.replaceString(6, 7, "B"); //full reparse
            //doc.insertString(19, "1");
            doc.insertString(24, "int a=1;");
        });
        var bs = BlockSupportImpl.getInstance(prj.getProject());
        //var textRange = new TextRange(6, 7);
        //var textRange = new TextRange(19, 20);
        var range1 = ChangedPsiRangeUtil.getChangedPsiRange(psiFile1, (FileElement) psiFile1.getNode(), doc.getImmutableCharSequence());
        var textRange = getChangedPsiRange2(psiFile1, doc, lastCommitted, doc.getImmutableCharSequence(),
                24, 0);
        //var textRange         = ProperTextRange.create(24, 24);
        var progressIndicator = new EmptyProgressIndicator();
        var diffLog = bs.reparseRange(psiFile1, psiFile1.getNode(), textRange, doc.getImmutableCharSequence(),
                progressIndicator, lastCommitted);
        //diffLog.doActualPsiChange(psiFile1);
        diffLog.performActualPsiChange(psiFile1);

        //测试重新ParseFile
        //FileContentUtilCore.reparseFiles(Collections.singleton(vf));
        //FileManager fileManager = ((PsiManagerEx)PsiManager.getInstance(prj.getProject())).getFileManager();
        //var viewProvider = fileManager.findCachedViewProvider(vf);
        //if (viewProvider != null) {
        //    fileManager.setViewProvider(vf, null);
        //}

        var psiFile2 = PsiManager.getInstance(prj.getProject()).findFile(vf);
        var clss2    = ((PsiJavaFileImpl) psiFile2).getClasses();

        assertSame(psiFile1, psiFile2);
        //assertEquals( "BA", ((PsiJavaFileImpl) psiFile2).getClasses()[0].getName());
    }

    private static ProperTextRange getChangedPsiRange(PsiFile file,
                                                      Document document,
                                                      CharSequence oldDocumentText,
                                                      CharSequence newDocumentText) {
        int psiLength = oldDocumentText.length();
        if (!file.getViewProvider().supportsIncrementalReparse(file.getLanguage())) {
            return new ProperTextRange(0, psiLength);
        }
        List<DocumentEvent> events = ((PsiDocumentManagerBase) PsiDocumentManager.getInstance(file.getProject()))
                .getEventsSinceCommit(document);
        int prefix            = Integer.MAX_VALUE;
        int suffix            = Integer.MAX_VALUE;
        int lengthBeforeEvent = psiLength;
        for (DocumentEvent event : events) {
            prefix            = Math.min(prefix, event.getOffset());
            suffix            = Math.min(suffix, lengthBeforeEvent - event.getOffset() - event.getOldLength());
            lengthBeforeEvent = lengthBeforeEvent - event.getOldLength() + event.getNewLength();
        }
        if ((prefix == psiLength || suffix == psiLength) && newDocumentText.length() == psiLength) {
            return null;
        }
        //Important! delete+insert sequence can give some of same chars back, lets grow affixes to include them.
        int shortestLength = Math.min(psiLength, newDocumentText.length());
        while (prefix < shortestLength && oldDocumentText.charAt(prefix) == newDocumentText.charAt(prefix)) {
            prefix++;
        }
        while (suffix < shortestLength - prefix &&
                oldDocumentText.charAt(psiLength - suffix - 1) == newDocumentText.charAt(newDocumentText.length() - suffix - 1)) {
            suffix++;
        }
        int end = Math.max(prefix, psiLength - suffix);
        if (end == prefix && newDocumentText.length() == oldDocumentText.length()) return null;
        return ProperTextRange.create(prefix, end);
    }

    private static ProperTextRange getChangedPsiRange2(PsiFile file,
                                                       Document document,
                                                       CharSequence oldDocumentText,
                                                       CharSequence newDocumentText,
                                                       int eventOffset, int eventOldLength) {
        int psiLength = oldDocumentText.length();
        int lengthBeforeEvent = psiLength;
        int prefix            = eventOffset;
        int suffix            = lengthBeforeEvent - eventOffset - eventOldLength;
        //lengthBeforeEvent = lengthBeforeEvent - eventOldLength + eventNewLength;

        if ((prefix == psiLength || suffix == psiLength) && newDocumentText.length() == psiLength) {
            return null;
        }
        //Important! delete+insert sequence can give some of same chars back, lets grow affixes to include them.
        int shortestLength = Math.min(psiLength, newDocumentText.length());
        while (prefix < shortestLength && oldDocumentText.charAt(prefix) == newDocumentText.charAt(prefix)) {
            prefix++;
        }
        while (suffix < shortestLength - prefix &&
                oldDocumentText.charAt(psiLength - suffix - 1) == newDocumentText.charAt(newDocumentText.length() - suffix - 1)) {
            suffix++;
        }
        int end = Math.max(prefix, psiLength - suffix);
        if (end == prefix && newDocumentText.length() == oldDocumentText.length()) return null;
        return ProperTextRange.create(prefix, end);
    }

    @Test
    public void testJarFile() throws IOException {
        var                   jarFile = new JarFile("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar");
        JarEntry              entry;
        Enumeration<JarEntry> e       = jarFile.entries();
        while (e.hasMoreElements()) {
            entry = e.nextElement();
            if (entry != null && !entry.isDirectory() && entry.getName().endsWith(".class")) {
                String name = entryPathToClassName(entry.getName());
                //classpathElements.put(name, new ClasspathElement(jarFile, entry));
            }
        }
    }

    private String entryPathToClassName(String entryPath) {
        if (!entryPath.endsWith(".class")) {
            throw new IllegalStateException();
        }
        String className = entryPath.substring(0, entryPath.length() - ".class".length());
        className = className.replace('/', '.');
        className = className.replace('$', '.');
        return className;
    }
}
