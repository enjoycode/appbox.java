import org.junit.jupiter.api.Test;
import javassist.*;

import java.io.IOException;

public class HackIdeaJar {

    //注意CtMethod.setBody()时参数用$1,$2表示
    //替换jar class: jar -uvf java-impl.jar com/intellij/codeInsight/completion/SlowerTypeConversions.class

    @Test
    public void hack() throws NotFoundException, CannotCompileException, IOException {
        var oldJarPath = "/media/psf/Home/Projects/AppBoxFuture/appbox.java/design/lib/java-impl.jar";
        var outClassPath = "/home/rick/out";

        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(oldJarPath);

        //处理SlowerTypeConversions.getItemText(PsiFile, Object)不关联CodeStyle.getLanguageSettings(PsiFile)
        CtClass  clsSlowerTypeConversions = pool.get("com.intellij.codeInsight.completion.SlowerTypeConversions");
        CtMethod mtdGetItemText           = clsSlowerTypeConversions.getDeclaredMethod("getItemText");
        var body = "{if ($2 instanceof com.intellij.psi.PsiMethod) {\n";
        body += "final com.intellij.psi.PsiMethod method = (com.intellij.psi.PsiMethod)$2;\n";
        body += "final com.intellij.psi.PsiType type = method.getReturnType();\n";
        body += "if (com.intellij.psi.PsiType.VOID.equals(type) || com.intellij.psi.PsiType.NULL.equals(type)) return null;\n";
        body += "if (!method.getParameterList().isEmpty()) return null;\n";
        body += "return method.getName() + \"()\";\n";
        body += "} else if ($2 instanceof com.intellij.psi.PsiVariable) {\n";
        body += " return ((com.intellij.psi.PsiVariable)$2).getName();\n}\n";
        body += "return null;\n}";
        mtdGetItemText.setBody(body);

        //save jar
        clsSlowerTypeConversions.writeFile(outClassPath);
    }

}
