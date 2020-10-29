package appbox.design.jdt;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class ModelContainer extends ModelResource implements IContainer {

    public ModelContainer(IPath path, ModelWorkspace workspace) {
        super(path, workspace);
    }

    @Override
    public boolean exists(IPath childPath) {
        return workspace.getResourceInfo(getFullPath().append(childPath), false, false) != null;
    }

    //region ====findMember====
    @Override
    public IResource findMember(String memberPath) {
        return findMember(memberPath, false);
    }

    @Override
    public IResource findMember(String memberPath, boolean phantom) {
        IPath childPath = getFullPath().append(memberPath);
        ResourceInfo info = workspace.getResourceInfo(childPath, phantom, false);
        return info == null ? null : workspace.newResource(childPath, info.getType());
    }

    @Override
    public IResource findMember(IPath childPath) {
        return findMember(childPath, false);
    }

    @Override
    public IResource findMember(IPath childPath, boolean phantom) {
        childPath = getFullPath().append(childPath);
        ResourceInfo info = workspace.getResourceInfo(childPath, phantom, false);
        return (info == null) ? null : workspace.newResource(childPath, info.getType());
    }
    //endregion

    @Override
    public String getDefaultCharset() throws CoreException {
        return null;
    }

    @Override
    public String getDefaultCharset(boolean b) throws CoreException {
        return null;
    }

    //region ====getFile & getFolder====
    @Override
    public IFile getFile(IPath childPath) {
        return (IFile) workspace.newResource(getFullPath().append(childPath), FILE);
    }

    public IFile getFile(String name) {
        return (IFile) workspace.newResource(getFullPath().append(name), FILE);
    }

    public IFolder getFolder(String name) {
        return (IFolder) workspace.newResource(getFullPath().append(name), FOLDER);
    }

    @Override
    public IFolder getFolder(IPath child) {
        return (IFolder) workspace.newResource(getFullPath().append(child), FOLDER);
    }
    //endregion

    //region ====members====
    @Override
    public IResource[] members() throws CoreException {
        return members(IResource.NONE);
    }

    @Override
    public IResource[] members(boolean phantom) throws CoreException {
        return members(phantom ? INCLUDE_PHANTOMS : IResource.NONE);
    }

    @Override
    public IResource[] members(int memberFlags) throws CoreException {
        final boolean phantom = (memberFlags & INCLUDE_PHANTOMS) != 0;
        ResourceInfo info = getResourceInfo(phantom, false);
        checkAccessible(getFlags(info));
        ////if children are currently unknown, ask for immediate refresh
        //if (info.isSet(ICoreConstants.M_CHILDREN_UNKNOWN))
        //    workspace.refreshManager.refresh(this);
        return getChildren(memberFlags);
    }
    //endregion

    @Override
    public IFile[] findDeletedMembersWithHistory(int i, IProgressMonitor iProgressMonitor) throws CoreException {
        return new IFile[0];
    }

    @Override
    public void setDefaultCharset(String s) throws CoreException {

    }

    @Override
    public void setDefaultCharset(String s, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public IResourceFilterDescription createFilter(int i, FileInfoMatcherDescription fileInfoMatcherDescription, int i1, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public IResourceFilterDescription[] getFilters() throws CoreException {
        return new IResourceFilterDescription[0];
    }

    protected IResource[] getChildren(int memberFlags) {
        IPath[] children = null;
        try {
            children = workspace.tree.getChildren(path);
        } catch (IllegalArgumentException e) {
            //concurrency problem: the container has been deleted by another
            //thread during this call.  Just return empty children set
        }
        if (children == null || children.length == 0)
            return ICoreConstants.EMPTY_RESOURCE_ARRAY;
        var result = new ModelResource[children.length];
        int found  = 0;
        for (IPath child : children) {
            ResourceInfo info = workspace.getResourceInfo(child, true, false);
            if (info != null && isMember(info.getFlags(), memberFlags))
                result[found++] = workspace.newResource(child, info.getType());
        }
        if (found == result.length)
            return result;
        var trimmedResult = new ModelResource[found];
        System.arraycopy(result, 0, trimmedResult, 0, found);
        return trimmedResult;
    }
}
