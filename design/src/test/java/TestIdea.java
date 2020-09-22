import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.LanguageFileViewProviders;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.JavaPsiFacadeImpl;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.file.impl.JavaFileManagerImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestIdea {

    @Test
    public void testIdea() {
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
        var psiFile = fileViewProvider.getPsi(JavaLanguage.INSTANCE);
        var childs = psiFile.getChildren();

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

}
