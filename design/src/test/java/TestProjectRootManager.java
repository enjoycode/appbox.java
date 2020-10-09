import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TestProjectRootManager extends ProjectRootManager {
    private final TestProjectFileIndex _fileIndex = new TestProjectFileIndex();

    @Override
    public ProjectFileIndex getFileIndex() {
        return _fileIndex;
    }

    @Override
    public OrderEnumerator orderEntries() {
        return null;
    }

    @Override
    public OrderEnumerator orderEntries(Collection<? extends Module> collection) {
        return null;
    }

    @Override
    public VirtualFile[] getContentRootsFromAllModules() {
        return new VirtualFile[0];
    }

    @Override
    public List<String> getContentRootUrls() {
        return null;
    }

    @Override
    public VirtualFile[] getContentRoots() {
        return new VirtualFile[0];
    }

    @Override
    public VirtualFile[] getContentSourceRoots() {
        return new VirtualFile[0];
    }

    @Override
    public List<VirtualFile> getModuleSourceRoots(Set<? extends JpsModuleSourceRootType<?>> set) {
        return null;
    }

    @Override
    public Sdk getProjectSdk() {
        return null;
    }

    @Override
    public String getProjectSdkName() {
        return null;
    }

    @Override
    public String getProjectSdkTypeName() {
        return null;
    }

    @Override
    public void setProjectSdk(Sdk sdk) {

    }

    @Override
    public void setProjectSdkName(String s) {

    }

    @Override
    public void setProjectSdkName(String s, String s1) {

    }
}
