package appbox.design.lang.java.jdt;

import appbox.design.utils.PathUtil;
import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public final class ModelFolder extends Folder {

    ModelFolder(IPath path, Workspace container) {
        super(path, container);
    }

    @Override
    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
        final var       workspace = (Workspace) getWorkspace();
        ISchedulingRule rule      = workspace.getRuleFactory().createRule(this);

        try {
            workspace.prepareOperation(rule, monitor);
            workspace.beginOperation(true);
            //TODO:internalCreate
            ((Workspace) getWorkspace()).createResource(this, updateFlags);
            //if (!local) {
            this.getResourceInfo(true, true).clearModificationStamp();
            //}
        } catch (OperationCanceledException ex) {
            workspace.getWorkManager().operationCanceled();
            throw ex;
        } finally {
            workspace.endOperation(rule, true);
        }
    }

    @Override
    public IPath getLocation() {
        return PathUtil.WORKSPACE_PATH.append(getFullPath());
    }

    @Override
    public String getDefaultCharset(boolean checkImplicit) {
        return "UTF8";
    }
}
