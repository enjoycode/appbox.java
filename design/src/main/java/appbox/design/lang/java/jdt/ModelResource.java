package appbox.design.lang.java.jdt;

import appbox.design.utils.PathUtil;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.WrappedRuntimeException;
import org.eclipse.core.internal.watson.ElementTreeIterator;
import org.eclipse.core.internal.watson.IElementContentVisitor;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.net.URI;
import java.util.Map;

public abstract class ModelResource implements IResource {
    IPath          path;
    ModelWorkspace workspace;

    public ModelResource(IPath path, ModelWorkspace workspace) {
        this.path      = path.removeTrailingSeparator();
        this.workspace = workspace;
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    public void createLink(URI localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    //region ====accept====
    @Override
    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        accept(visitor, IResource.DEPTH_INFINITE, memberFlags);
    }

    @Override
    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
        // It is invalid to call accept on a phantom when INCLUDE_PHANTOMS is not specified.
        final boolean includePhantoms = (memberFlags & IContainer.INCLUDE_PHANTOMS) != 0;
        if ((memberFlags & IContainer.DO_NOT_CHECK_EXISTENCE) == 0)
            checkAccessible(getFlags(getResourceInfo(includePhantoms, false)));

        final var proxy = new ModelResourceProxy(this.workspace);
        IElementContentVisitor elementVisitor = (tree, requestor, contents) -> {
            ResourceInfo info = (ResourceInfo) contents;
            if (!isMember(getFlags(info), memberFlags))
                return false;
            proxy.requestor = requestor;
            proxy.info      = info;
            try {
                boolean shouldContinue = true;
                switch (depth) {
                    case DEPTH_ZERO:
                        shouldContinue = false;
                        break;
                    case DEPTH_ONE:
                        shouldContinue = !path.equals(requestor.requestPath().removeLastSegments(1));
                        break;
                    case DEPTH_INFINITE:
                        shouldContinue = true;
                        break;
                }
                return visitor.visit(proxy) && shouldContinue;
            } catch (CoreException e) {
                // Throw an exception to bail out of the traversal.
                throw new WrappedRuntimeException(e);
            } finally {
                proxy.reset();
            }
        };
        try {
            new ElementTreeIterator(workspace.getElementTree(), getFullPath()).iterate(elementVisitor);
        } catch (WrappedRuntimeException e) {
            throw (CoreException) e.getTargetException();
        } finally {
            proxy.requestor = null;
            proxy.info      = null;
        }
    }

    @Override
    public void accept(IResourceVisitor visitor) throws CoreException {
        accept(visitor, IResource.DEPTH_INFINITE, 0);
    }

    @Override
    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        accept(visitor, depth, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
    }

    @Override
    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new RuntimeException("未实现");
    }
    //endregion

    @Override
    public void clearHistory(IProgressMonitor iProgressMonitor) throws CoreException {

    }

    //region ====copy====
    @Override
    public void copy(IPath iPath, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void copy(IPath iPath, int i, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void copy(IProjectDescription iProjectDescription, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void copy(IProjectDescription iProjectDescription, int i, IProgressMonitor iProgressMonitor) throws CoreException {

    }
    //endregion

    @Override
    public IMarker createMarker(String type) throws CoreException {
        MarkerInfo info = new MarkerInfo();
        info.setType(type);
        info.setCreationTime(System.currentTimeMillis());
        //this.workspace.getMarkerManager().add(this, info);
        return new ModelMarker(this, info.getId());
    }

    @Override
    public IResourceProxy createProxy() {
        throw new RuntimeException("未实现");
        //var result = new ModelResourceProxy(this.workspace);
        //result.info = getResourceInfo(false, false);
        //result.requestor = this;
        //result.resource = this;
        //return result;
    }

    //region ====delete====
    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        int updateFlags = force ? IResource.FORCE : IResource.NONE;
        updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
        delete(updateFlags, monitor);
    }

    @Override
    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        delete(force ? IResource.FORCE : IResource.NONE, monitor);
    }

    @Override
    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        //TODO:暂简单实现,需要删除编译临时文件
        workspace.deleteResource(this);
    }
    //endregion

    @Override
    public void deleteMarkers(String s, boolean b, int i) throws CoreException {

    }

    @Override
    public boolean exists() {
        ResourceInfo info = getResourceInfo(false, false);
        return exists(getFlags(info), true);
    }

    public boolean exists(int flags, boolean checkType) {
        return flags != ICoreConstants.NULL_FLAG && !(checkType && ResourceInfo.getType(flags) != getType());
    }

    @Override
    public IMarker findMarker(long l) throws CoreException {
        return null;
    }

    @Override
    public IMarker[] findMarkers(String s, boolean b, int i) throws CoreException {
        return new IMarker[0];
    }

    @Override
    public int findMaxProblemSeverity(String s, boolean b, int i) throws CoreException {
        return 0;
    }

    @Override
    public String getFileExtension() {
        String name  = getName();
        int    index = name.lastIndexOf('.');
        if (index == -1)
            return null;
        if (index == (name.length() - 1))
            return "";
        return name.substring(index + 1);
    }

    @Override
    public IPath getFullPath() {
        return path;
    }

    @Override
    public long getLocalTimeStamp() {
        return 0;
    }

    @Override
    public IPath getLocation() {
        //注意返回指向临时目录, eg: /tmp/appbox/workspace/sessionid
        var workingroot = PathUtil.getWorkingLocation(workspace.languageServer.sessionId);
        return workingroot.append(this.path);
    }

    @Override
    public URI getLocationURI() {
        //简单返回, CompletionProposalRequestor.toCompleteItem需要
        return null;//return URI.create("model:" + path.toString());
    }

    @Override
    public IMarker getMarker(long l) {
        return null;
    }

    @Override
    public long getModificationStamp() {
        return 0;
    }

    @Override
    public String getName() {
        return path.lastSegment();
    }

    @Override
    public IPathVariableManager getPathVariableManager() {
        return null;
    }

    @Override
    public IContainer getParent() {
        int segments = path.segmentCount();
        //// Zero and one segments handled by subclasses.
        //if (segments < 2)
        //    Assert.isLegal(false, path.toString());
        if (segments == 2)
            return workspace.getRoot().getProject(path.segment(0));
        return (IFolder) workspace.newResource(path.removeLastSegments(1), IResource.FOLDER);
    }

    @Override
    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
        return null;
    }

    @Override
    public String getPersistentProperty(QualifiedName qualifiedName) throws CoreException {
        return null;
    }

    @Override
    public IProject getProject() {
        return workspace.getRoot().getProject(this.path.segment(0));
    }

    @Override
    public IPath getProjectRelativePath() {
        return this.getFullPath().removeFirstSegments(1);
    }

    @Override
    public IPath getRawLocation() {
        return null;
    }

    @Override
    public URI getRawLocationURI() {
        return null;
    }

    @Override
    public ResourceAttributes getResourceAttributes() {
        return null;
    }

    @Override
    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
        return null;
    }

    @Override
    public Object getSessionProperty(QualifiedName qualifiedName) throws CoreException {
        return null;
    }

    @Override
    public IWorkspace getWorkspace() {
        return workspace;
    }

    public int getFlags(ResourceInfo info) {
        return (info == null) ? ICoreConstants.NULL_FLAG : info.getFlags();
    }

    @Override
    public boolean isAccessible() {
        return exists();
    }

    @Override
    public boolean isDerived() {
        return false;
    }

    @Override
    public boolean isDerived(int i) {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isHidden(int i) {
        return false;
    }

    @Override
    public boolean isLinked() {
        return false;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean isLinked(int i) {
        return false;
    }

    @Override
    public boolean isLocal(int i) {
        return false;
    }

    @Override
    public boolean isPhantom() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isSynchronized(int i) {
        return false;
    }

    @Override
    public boolean isTeamPrivateMember() {
        return false;
    }

    @Override
    public boolean isTeamPrivateMember(int i) {
        return false;
    }

    //region ====move====
    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        int updateFlags = force ? IResource.FORCE : IResource.NONE;
        updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
        move(destination, updateFlags, monitor);
    }

    @Override
    public void move(IPath iPath, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(IPath iPath, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(IProjectDescription iProjectDescription, boolean b, boolean b1, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(IProjectDescription iProjectDescription, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }
    //endregion

    @Override
    public void refreshLocal(int i, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void revertModificationStamp(long l) throws CoreException {

    }

    @Override
    public void setDerived(boolean b) throws CoreException {

    }

    @Override
    public void setDerived(boolean b, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void setHidden(boolean b) throws CoreException {

    }

    @Override
    public void setLocal(boolean b, int i, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public long setLocalTimeStamp(long l) throws CoreException {
        return 0;
    }

    @Override
    public void setPersistentProperty(QualifiedName qualifiedName, String s) throws CoreException {

    }

    @Override
    public void setReadOnly(boolean b) {

    }

    @Override
    public void setResourceAttributes(ResourceAttributes resourceAttributes) throws CoreException {

    }

    @Override
    public void setSessionProperty(QualifiedName qualifiedName, Object o) throws CoreException {

    }

    @Override
    public void setTeamPrivateMember(boolean b) throws CoreException {

    }

    @Override
    public void touch(IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public <T> T getAdapter(Class<T> aClass) {
        return null;
    }

    @Override
    public boolean contains(ISchedulingRule iSchedulingRule) {
        return false;
    }

    @Override
    public boolean isConflicting(ISchedulingRule iSchedulingRule) {
        return false;
    }

    public void checkAccessible(int flags) throws CoreException {
        checkExists(flags, true);
    }

    /**
     * Checks that this resource exists.
     * If checkType is true, the type of this resource and the one in the tree must match.
     * @throws CoreException if this resource does not exist
     */
    public void checkExists(int flags, boolean checkType) throws CoreException {
        if (!exists(flags, checkType)) {
            String message = Messages.resources_mustExist + ":" + getFullPath();
            throw new ResourceException(IResourceStatus.RESOURCE_NOT_FOUND, getFullPath(), message, null);
        }
    }

    /**
     * Returns the resource info.  Returns null if the resource doesn't exist.
     * If the phantom flag is true, phantom resources are considered.
     * If the mutable flag is true, a mutable info is returned.
     */
    public ResourceInfo getResourceInfo(boolean phantom, boolean mutable) {
        return workspace.getResourceInfo(getFullPath(), phantom, mutable);
    }

    public String getTypeString() {
        switch (getType()) {
            case FILE:
                return "L";
            case FOLDER:
                return "F";
            case PROJECT:
                return "P";
            case ROOT:
                return "R";
        }
        return "";
    }

    protected boolean isMember(int flags, int memberFlags) {
        int excludeMask = 0;
        if ((memberFlags & IContainer.INCLUDE_PHANTOMS) == 0)
            excludeMask |= ICoreConstants.M_PHANTOM;
        if ((memberFlags & IContainer.INCLUDE_HIDDEN) == 0)
            excludeMask |= ICoreConstants.M_HIDDEN;
        if ((memberFlags & IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS) == 0)
            excludeMask |= ICoreConstants.M_TEAM_PRIVATE_MEMBER;
        if ((memberFlags & IContainer.EXCLUDE_DERIVED) != 0)
            excludeMask |= ICoreConstants.M_DERIVED;
        // The resource is a matching member if it matches none of the exclude flags.
        return flags != ICoreConstants.NULL_FLAG && (flags & excludeMask) == 0;
    }

    @Override
    public boolean equals(Object target) {
        if (this == target)
            return true;
        if (!(target instanceof ModelResource))
            return false;
        var resource = (ModelResource) target;
        return getType() == resource.getType()
                && path.equals(resource.path) /*&& workspace.equals(resource.workspace)*/;
    }
}
