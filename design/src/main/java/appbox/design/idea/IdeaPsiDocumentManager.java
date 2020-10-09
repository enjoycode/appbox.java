package appbox.design.idea;

import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.PsiDocumentManagerBase;

public final class IdeaPsiDocumentManager extends PsiDocumentManagerBase {
    IdeaPsiDocumentManager(Project project) {
        super(project);
    }
}
