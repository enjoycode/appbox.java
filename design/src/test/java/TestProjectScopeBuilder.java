import com.intellij.core.CoreProjectScopeBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.psi.search.GlobalSearchScope;

public class TestProjectScopeBuilder extends CoreProjectScopeBuilder {
    public TestProjectScopeBuilder(Project project, FileIndexFacade fileIndexFacade) {
        super(project, fileIndexFacade);
    }

    @Override
    public GlobalSearchScope buildLibrariesScope() {
        return super.buildLibrariesScope();
    }

    @Override
    public GlobalSearchScope buildAllScope() {
        return super.buildAllScope();
    }

    @Override
    public GlobalSearchScope buildProjectScope() {
        return super.buildProjectScope();
    }

    @Override
    public GlobalSearchScope buildContentScope() {
        return super.buildContentScope();
    }

    @Override
    public GlobalSearchScope buildEverythingScope() {
        return super.buildEverythingScope();
    }
}
