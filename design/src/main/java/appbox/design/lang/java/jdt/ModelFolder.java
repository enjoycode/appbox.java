package appbox.design.lang.java.jdt;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class ModelFolder extends ModelContainer implements IFolder {

    public ModelFolder(IPath path, ModelWorkspace workspace) {
        super(path, workspace);
    }

    @Override
    public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
        create(force ? 1 : 0, local, monitor);
    }

    @Override
    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
        //TODO:internalCreate
        workspace.createResource(this, updateFlags);
        //if (!local) {
        this.getResourceInfo(true, true).clearModificationStamp();
        //}
    }

    @Override
    public int getType() {
        return IResource.FOLDER;
    }
}
