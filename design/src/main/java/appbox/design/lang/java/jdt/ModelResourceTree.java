package appbox.design.lang.java.jdt;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ILock;

final class ModelResourceTree implements IResourceTree {
    private boolean     isValid = true;
    /** The lock to acquire when the workspace needs to be manipulated */
    private ILock       lock;
    private MultiStatus multistatus;
    private int         updateFlags;

    public ModelResourceTree(ILock lock, MultiStatus status, int updateFlags) {
        this.lock        = lock;
        this.multistatus = status;
        this.updateFlags = updateFlags;
    }

    /**
     * The specific operation for which this tree was created has completed and this tree
     * should not be used anymore. Ensure that this is the case by making it invalid. This
     * is checked by all API methods.
     */
    void makeInvalid() {
        this.isValid = false;
    }

    @Override
    public void addToLocalHistory(IFile iFile) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public boolean isSynchronized(IResource iResource, int i) {
        return true;
    }

    @Override
    public long computeTimestamp(IFile iFile) {
        return 0;
    }

    @Override
    public long getTimestamp(IFile iFile) {
        return 0;
    }

    @Override
    public void updateMovedFileTimestamp(IFile iFile, long l) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void failed(IStatus reason) {
        multistatus.add(reason);
    }

    /** Returns the status object held onto by this resource tree. */
    IStatus getStatus() {
        return multistatus;
    }

    @Override
    public void deletedFile(IFile file) {
        Assert.isLegal(isValid);
        try {
            lock.acquire();
            // Do nothing if the resource doesn't exist.
            if (!file.exists())
                return;
            try {
                // Delete properties, generate marker deltas, and remove the node from the workspace tree.
                ((Resource) file).deleteResource(true, null);
            } catch (CoreException e) {
                String  message = "File.deleteResource";
                IStatus status  = new ResourceStatus(IStatus.ERROR, file.getFullPath(), message, e);
                failed(status);
            }
        } finally {
            lock.release();
        }
    }

    @Override
    public void deletedFolder(IFolder folder) {
        Assert.isLegal(isValid);
        try {
            lock.acquire();
            // Do nothing if the resource doesn't exist.
            if (!folder.exists())
                return;
            try {
                // Delete properties, generate marker deltas, and remove the node from the workspace tree.
                ((Resource) folder).deleteResource(true, null);
            } catch (CoreException e) {
                String  message = "Folder.deleteResource";
                IStatus status  = new ResourceStatus(IStatus.ERROR, folder.getFullPath(), message, e);
                failed(status);
            }
        } finally {
            lock.release();
        }
    }

    @Override
    public void deletedProject(IProject project) {
        Assert.isLegal(isValid);
        try {
            lock.acquire();
            // Do nothing if the resource doesn't exist.
            if (!project.exists())
                return;
            // Delete properties, generate marker deltas, and remove the node from the workspace tree.
            try {
                ((Project) project).deleteResource(false, null);
            } catch (CoreException e) {
                String  message = "Project.deleteResource";
                IStatus status  = new ResourceStatus(IStatus.ERROR, project.getFullPath(), message, e);
                // log the status but don't return until we try and delete the rest of the project info
                failed(status);
            }
        } finally {
            lock.release();
        }
    }

    @Override
    public void movedFile(IFile iFile, IFile iFile1) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void movedFolderSubtree(IFolder iFolder, IFolder iFolder1) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public boolean movedProjectSubtree(IProject iProject, IProjectDescription iProjectDescription) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void standardDeleteFile(IFile file, int flags, IProgressMonitor monitor) {
        Assert.isLegal(isValid);
        try {
            lock.acquire();
            internalDeleteFile(file, flags, monitor);
        } finally {
            lock.release();
        }
    }

    @Override
    public void standardDeleteFolder(IFolder folder, int flags, IProgressMonitor monitor) {
        Assert.isLegal(isValid);
        try {
            lock.acquire();
            internalDeleteFolder(folder, flags, monitor);
        } catch (OperationCanceledException oce) {
            safeRefresh(folder);
            throw oce;
        } finally {
            lock.release();
            monitor.done();
        }
    }

    @Override
    public void standardDeleteProject(IProject project, int flags, IProgressMonitor monitor) {
        Assert.isLegal(isValid);
        try {
            lock.acquire();
            // Do nothing if the project doesn't exist in the workspace tree.
            if (!project.exists())
                return;

            boolean alwaysDeleteContent = (flags & IResource.ALWAYS_DELETE_PROJECT_CONTENT) != 0;
            boolean neverDeleteContent  = (flags & IResource.NEVER_DELETE_PROJECT_CONTENT) != 0;
            boolean success             = true;

            // Delete project content.  Don't do anything if the user specified explicitly asked
            // not to delete the project content or if the project is closed and
            // ALWAYS_DELETE_PROJECT_CONTENT was not specified.
            if (alwaysDeleteContent || (project.isOpen() && !neverDeleteContent)) {
                // Force is implied if alwaysDeleteContent is true or if the project is in sync
                // with the local file system.
                if (alwaysDeleteContent || isSynchronized(project, IResource.DEPTH_INFINITE)) {
                    flags |= IResource.FORCE;
                }

                // If the project is open we have to recursively try and delete all the files doing best-effort.
                if (project.isOpen()) {
                    success = internalDeleteProject(project, flags, monitor);
                    if (!success) {
                        var     message = "internal delete project failed.";
                        IStatus status  = new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, project.getFullPath(), message);
                        failed(status);
                    }
                    return;
                }

                // If the project is closed we can short circuit this operation and delete all the files on disk.
                // The .project file is deleted at the end of the operation.
                throw new RuntimeException("Will never be here");
            }

            // Signal that the workspace tree should be updated that the project has been deleted.
            //if (success)
            deletedProject(project);
        } finally {
            lock.release();
        }
    }

    /** Does a best-effort delete on this resource and all its children. */
    private boolean internalDeleteProject(IProject project, int flags, IProgressMonitor monitor) {
        // Recursively delete each member of the project.
        IResource[] members = null;
        try {
            members = project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
        } catch (CoreException e) {
            String  message = "Can't get project members";
            IStatus status  = new ResourceStatus(IStatus.ERROR, project.getFullPath(), message, e);
            failed(status);
            // Indicate that the delete was unsuccessful.
            return false;
        }
        boolean deletedChildren = true;
        for (IResource member : members) {
            IResource child = member;
            switch (child.getType()) {
                case IResource.FILE:
                    // ignore the .project file for now and delete it last
                    if (!IProjectDescription.DESCRIPTION_FILE_NAME.equals(child.getName()))
                        deletedChildren &= internalDeleteFile((IFile) child, flags, monitor);
                    break;
                case IResource.FOLDER:
                    deletedChildren &= internalDeleteFolder((IFolder) child, flags, monitor);
                    break;
            }
        }
        // Check to see if the children were deleted ok. If there was a problem
        // just return as the problem should have been logged by the recursive
        // call to the child.
        if (!deletedChildren)
            // Indicate that the delete was unsuccessful.
            return false;

        //children are deleted, so now delete the parent
        deletedProject(project);
        return true;
    }

    /**
     * Helper method for #standardDeleteFolder. Returns a boolean indicating
     * whether or not the deletion of this folder was successful. Does a best effort
     * delete of this resource and its children.
     */
    private boolean internalDeleteFolder(IFolder folder, int flags, IProgressMonitor monitor) {
        // Do nothing if the folder doesn't exist in the workspace.
        if (!folder.exists())
            return true;

        //// Don't delete contents if this is a linked resource
        //if (folder.isLinked()) {
        //    deletedFolder(folder);
        //    return true;
        //}

        deletedFolder(folder);
        //TODO:删除编译生成的目录
        return true;
    }

    /**
     * Helper method for #standardDeleteFile. Returns a boolean indicating whether or
     * not the delete was successful.
     */
    private boolean internalDeleteFile(IFile file, int flags, IProgressMonitor monitor) {
        // Do nothing if the file doesn't exist in the workspace.
        if (!file.exists()) {
            // Indicate that the delete was successful.
            return true;
        }

        //// Don't delete contents if this is a linked resource
        //if (file.isLinked()) {
        //    deletedFile(file);
        //    return true;
        //}

        // If the file doesn't exist on disk then signal to the workspace to delete the
        // file and return.
        deletedFile(file);
        //TODO:删除编译生成的文件
        return true;
    }

    @Override
    public void standardMoveFile(IFile source, IFile destination, int i, IProgressMonitor monitor) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void standardMoveFolder(IFolder source, IFolder destination, int i, IProgressMonitor monitor) {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void standardMoveProject(IProject project, IProjectDescription description, int i, IProgressMonitor monitor) {
        throw new RuntimeException("Not supported.");
    }

    /**
     * Refreshes the resource hierarchy with its children. In case of failure
     * adds an appropriate status to the resource tree's status.
     */
    private void safeRefresh(IResource resource) {
        try {
            resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException ce) {
            IStatus status = new ResourceStatus(IStatus.ERROR, IResourceStatus.FAILED_DELETE_LOCAL,
                    resource.getFullPath(), "Resource.refreshLocal error", ce);
            failed(status);
        }
    }
}
