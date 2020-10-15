import appbox.design.idea.*;
import appbox.model.ServiceModel;
import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.codeInsight.ExternalAnnotationsManagerImpl;
import com.intellij.codeInsight.InferredAnnotationsManager;
import com.intellij.codeInsight.InferredAnnotationsManagerImpl;
import com.intellij.codeInsight.completion.*;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.MockDocumentEvent;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.*;
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

import javax.swing.*;
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
    public void testCompletion() { //测试代码提示
        var prj = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);

        prj.addJarToClassPath(new File("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar"));
        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java",
                "package s.impl; class A {A b; void say(){b.toString();}}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psiFile = PsiManager.getInstance(prj.getProject()).findFile(file1);
        //for (int i = 0; i < 10; i++) {
        var cursor      = 43;
        var position    = psiFile.findElementAt(cursor);
        var contributor = new JavaCompletionContributor();

        var cParameters = new CompletionParameters(position, psiFile, CompletionType.BASIC,
                cursor, 0, new IdeaEditor(), () -> false);
        var cResultSet = new IdeaCompletionResultSet(
                completionResult -> System.out.println(completionResult),
                PrefixMatcher.ALWAYS_TRUE, contributor, cParameters, null, null);

        contributor.fillCompletionVariants(cParameters, cResultSet);
        //}
    }

    @Test
    public void testReparse() { //测试修改代码后incremental reparse
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
    public void testCodeStyle() {
        var prj = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);

        prj.addJarToClassPath(new File("/media/psf/Home/Projects/intellij-community/java/mockJDK-11/jre/lib/rt.jar"));
        var root = new TestVirtualFile("", System.currentTimeMillis());
        var file1 = new TestVirtualFile("A.java", "class TT {}", System.currentTimeMillis());
        prj.addSourcesToClasspath(root);

        var psiFile = PsiManager.getInstance(prj.getProject()).findFile(file1);

        prj.getProject().registerService(InferredAnnotationsManager.class, InferredAnnotationsManagerImpl.class);
        var obj1 = ExternalAnnotationsManager.getInstance(prj.getProject());
        var obj2 = InferredAnnotationsManager.getInstance(prj.getProject());

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
