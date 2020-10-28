import appbox.design.idea.*;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.*;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.daemon.*;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInsight.daemon.impl.TextEditorHighlightingPassRegistrarImpl;
import com.intellij.codeInspection.ex.ApplicationInspectionProfileManager;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.packageDependencies.DependencyValidationManagerImpl;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import static org.junit.jupiter.api.Assertions.*;

public class TestIdea {

    @Test
    @DisplayName("测试代码提示")
    public void testCompletion() {
        var prj  = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);
        var root = new TestVirtualFile("", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);
        //add files
        var dir2 = new TestVirtualFile("pack", System.currentTimeMillis());
        root.addChild(dir2);
        var file2 = new TestVirtualFile("B.java", "package pack;public class B{}", System.currentTimeMillis());
        dir2.addChild(file2);
        var file3 = new TestVirtualFile("C.java", "package pack;public class C{}", System.currentTimeMillis());
        dir2.addChild(file3);

        //var src    = "import m<caret>;class A{void say(){}}";
        //var src    = "class A{void say(){var o=new pack.m<caret>}}";
        var src = "class A{void say(){var o=new pack.m<caret>}}";
        //var prefixMatcher = new PlainPrefixMatcher("pac");
        var prefixMatcher = PrefixMatcher.ALWAYS_TRUE;
        var cursor        = src.indexOf("<caret>") - 1;
        var file1 = new TestVirtualFile("A.java",
                src.replace("<caret>", ""), System.currentTimeMillis());
        root.addChild(file1);

        var psiFile     = PsiManager.getInstance(prj.getProject()).findFile(file1);
        var position    = psiFile.findElementAt(cursor);
        var contributor = new JavaCompletionContributor();
        var cParameters = new CompletionParameters(position, psiFile, CompletionType.BASIC,
                cursor, 0, new IdeaEditor(), () -> false);
        var cResultSet = new IdeaCompletionResultSet(
                completionResult -> System.out.println(completionResult),
                prefixMatcher, contributor, cParameters, null, null);
        contributor.fillCompletionVariants(cParameters, cResultSet);
    }

    @Test
    @DisplayName("测试修改代码后incremental reparse")
    public void testReparse() {
        var prj = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);
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

    @Test
    @DisplayName("测试代码问题")
    public void testProblems() {
        var prj = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);
        var vf = new TestVirtualFile("A.java", "class A {void say(){}",
                System.currentTimeMillis());
        var psiDocumentManager =
                (IdeaPsiDocumentManager) PsiDocumentManager.getInstance(prj.getProject());

        var psiFile = PsiManager.getInstance(prj.getProject()).findFile(vf);
        var doc = FileDocumentManager.getInstance().getDocument(vf);
        //var model = DocumentMarkupModel.forDocument(doc, prj.getProject(), true);

        IdeaApplicationEnvironment.INSTANCE.registerApplicationService(DaemonCodeAnalyzerSettings.class,
                new DaemonCodeAnalyzerSettingsImpl());
        IdeaApplicationEnvironment.registerApplicationExtensionPoint(TextEditorHighlightingPassRegistrarImpl.EP_NAME,
                        TextEditorHighlightingPassFactoryRegistrar.class);
        prj.getProject().registerService(NamedScopeManager.class, NamedScopeManager.class);
        prj.getProject().registerService(DependencyValidationManager.class, DependencyValidationManagerImpl.class);
        prj.getProject().registerService(TextEditorHighlightingPassRegistrar.class,
                TextEditorHighlightingPassRegistrarImpl.class);
        prj.getProject().registerService(InspectionProjectProfileManager.class, ProjectInspectionProfileManager.class);
        prj.getProject().registerService(DaemonCodeAnalyzer.class, DaemonCodeAnalyzerImpl.class);
        //var analyzer = new DaemonCodeAnalyzerImpl(prj.getProject());
        //var problems = analyzer.getFileLevelHighlights(prj.getProject(), psiFile);
        var p = new ProjectInspectionProfileManager(prj.getProject());
        var problems = DaemonCodeAnalyzerImpl.getHighlights(doc,
                HighlightSeverity.ERROR, prj.getProject());
    }

    private static ProperTextRange getChangedPsiRange2(PsiFile file,
                                                       Document document,
                                                       CharSequence oldDocumentText,
                                                       CharSequence newDocumentText,
                                                       int eventOffset, int eventOldLength) {
        int psiLength         = oldDocumentText.length();
        int lengthBeforeEvent = psiLength;
        int prefix            = eventOffset;
        int suffix            = lengthBeforeEvent - eventOffset - eventOldLength;
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

    @Test
    public void testCodeStyle() {
        var prj = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);

        var root  = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java", "class TT {}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psiFile = (PsiJavaFile) PsiManager.getInstance(prj.getProject()).findFile(file1);
        var clss    = psiFile.getClasses();

        prj.getProject().registerService(InferredAnnotationsManager.class, InferredAnnotationsManagerImpl.class);
        var s1 = ExternalAnnotationsManager.getInstance(prj.getProject());
        var s2 = InferredAnnotationsManager.getInstance(prj.getProject());
        var s3 = NullableNotNullManager.getInstance(prj.getProject());

        IdeaApplicationEnvironment.registerApplicationExtensionPoint(FileTypeIndentOptionsProvider.EP_NAME, FileTypeIndentOptionsProvider.class);
        IdeaApplicationEnvironment.registerApplicationExtensionPoint(FileIndentOptionsProvider.EP_NAME, FileIndentOptionsProvider.class);
        IdeaApplicationEnvironment.registerApplicationExtensionPoint(LanguageCodeStyleSettingsProvider.EP_NAME, LanguageCodeStyleSettingsProvider.class);
        IdeaApplicationEnvironment.registerApplicationExtensionPoint(CodeStyleSettingsProvider.EXTENSION_POINT_NAME, CodeStyleSettingsProvider.class);
        IdeaApplicationEnvironment.registerApplicationExtensionPoint(FileCodeStyleProvider.EP_NAME, FileCodeStyleProvider.class);
        IdeaApplicationEnvironment.INSTANCE.getApplication().registerService(AppCodeStyleSettingsManager.class);

        //var s = CodeStyle.getLanguageSettings(psiFile);
        //var s2 = prj.getProject().getService(ProjectCodeStyleSettingsManager.class);
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
