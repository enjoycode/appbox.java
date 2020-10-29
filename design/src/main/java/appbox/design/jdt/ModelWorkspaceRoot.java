package appbox.design.jdt;

import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModelWorkspaceRoot extends ModelContainer implements IWorkspaceRoot {

    private final Map<String, IProject> projectTable = Collections.synchronizedMap(new HashMap<>(8));
    private final IPath                 workspaceLocation;

    public ModelWorkspaceRoot(IPath path, ModelWorkspace workspace) {
        super(path, workspace);
        this.workspaceLocation = (IPath) path.clone(); //FileUtil.canonicalPath(Platform.getLocation());
    }

    @Override
    public IPath getLocation() {
        return workspaceLocation;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void delete(boolean b, boolean b1, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IContainer[] findContainersForLocation(IPath iPath) {
        return new IContainer[0];
    }

    @Override
    public IContainer[] findContainersForLocationURI(URI uri) {
        return new IContainer[0];
    }

    @Override
    public IContainer[] findContainersForLocationURI(URI uri, int i) {
        return new IContainer[0];
    }

    @Override
    public IFile[] findFilesForLocation(IPath iPath) {
        return new IFile[0];
    }

    @Override
    public IFile[] findFilesForLocationURI(URI uri) {
        return new IFile[0];
    }

    @Override
    public IFile[] findFilesForLocationURI(URI uri, int i) {
        return new IFile[0];
    }

    @Override
    public IContainer getContainerForLocation(IPath iPath) {
        return null;
    }

    @Override
    public IFile getFileForLocation(IPath iPath) {
        return null;
    }

    //region ====getProject(s)=====
    @Override
    public IProject getProject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProject getProject(String name) {
        var result = projectTable.get(name);
        if (result == null) {
            IPath projectPath = (new ModelPath(null, name)).makeAbsolute();
            result = new ModelProject(projectPath, workspace);
            projectTable.put(name, result);
        }
        return result;
    }

    @Override
    public IProject[] getProjects() {
        return getProjects(IResource.NONE);
    }

    @Override
    public IProject[] getProjects(int memberFlags) {
        IResource[] roots  = getChildren(memberFlags);
        IProject[]  result = new IProject[roots.length];
        try {
            System.arraycopy(roots, 0, result, 0, roots.length);
        } catch (ArrayStoreException ex) {
            // Shouldn't happen since only projects should be children of the workspace root
            throw ex;
        }
        return result;
    }
    //endregion

    @Override
    public int getType() {
        return IResource.ROOT;
    }
}
