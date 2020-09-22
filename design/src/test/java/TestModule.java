import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.SystemIndependent;
import org.picocontainer.PicoContainer;

public final class TestModule extends UserDataHolderBase implements Module {
    private final Project _project;
    private       String  _name = "TestModule";

    public TestModule(Project project, String name) {
        _project = project;
        _name    = name;
    }

    @Override
    public VirtualFile getModuleFile() {
        return null;
    }

    @Override
    public @SystemIndependent String getModuleFilePath() {
        return null;
    }

    @Override
    public Project getProject() {
        return _project;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public <T> T getComponent(Class<T> aClass) {
        return null;
    }

    @Override
    public PicoContainer getPicoContainer() {
        return _project.getPicoContainer();
    }

    @Override
    public MessageBus getMessageBus() {
        return null;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public Condition<?> getDisposed() {
        return null;
    }

    @Override
    public boolean isLoaded() {
        return !isDisposed();
    }

    @Override
    public void setOption(String s, String s1) {

    }

    @Override
    public String getOptionValue(String s) {
        return null;
    }

    @Override
    public GlobalSearchScope getModuleScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleScope(boolean b) {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleWithLibrariesScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleWithDependenciesScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleContentScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleContentWithDependenciesScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean b) {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleWithDependentsScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleTestsWithDependentsScope() {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public GlobalSearchScope getModuleRuntimeScope(boolean b) {
        throw new UnsupportedOperationException("Method is not yet implemented in " + getClass().getName());
    }

    @Override
    public void dispose() {

    }
}
