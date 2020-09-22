import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.PsiDocumentManagerBase;

public final class TestPsiDocumentManager extends PsiDocumentManagerBase {
    protected TestPsiDocumentManager(Project project) {
        super(project);
    }
}
