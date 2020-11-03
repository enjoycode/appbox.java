package appbox.design.jdt;

import appbox.design.services.code.LanguageServer;
import appbox.design.utils.ReflectUtil;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class ModelWorkspace implements IWorkspace {

    protected          IWorkspaceRoot       defaultRoot = new ModelWorkspaceRoot(new ModelPath("/"), this);
    private            IResourceRuleFactory ruleFactory;
    protected          WorkspacePreferences description;
    protected volatile ElementTree          tree;
    //protected volatile Thread treeLocked = null;
    protected LocalMetaArea localMetaArea;
    protected long nextMarkerId = 0;
    protected long nextNodeId   = 1;

    public final LanguageServer languageServer;

    public ModelWorkspace(LanguageServer languageServer) {
        this.languageServer = languageServer;

        tree = new ElementTree();
        /* tree should only be modified during operations */
        //tree.immutable();
        //treeLocked = Thread.currentThread();
        tree.setTreeData(newElement(IResource.ROOT));

        //description = new WorkspacePreferences();
    }

    /**
     * Returns the current element tree for this workspace
     */
    public ElementTree getElementTree() {
        return tree;
    }

    @Override
    public void addResourceChangeListener(IResourceChangeListener iResourceChangeListener) {

    }

    @Override
    public void addResourceChangeListener(IResourceChangeListener iResourceChangeListener, int i) {

    }

    @Override
    public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant iSaveParticipant) throws CoreException {
        return null;
    }

    @Override
    public ISavedState addSaveParticipant(String s, ISaveParticipant iSaveParticipant) throws CoreException {
        return null;
    }

    @Override
    public void build(int i, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void build(IBuildConfiguration[] iBuildConfigurations, int i, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void checkpoint(boolean b) {

    }

    @Override
    public IProject[][] computePrerequisiteOrder(IProject[] iProjects) {
        return new IProject[0][];
    }

    @Override
    public ProjectOrder computeProjectOrder(IProject[] iProjects) {
        return null;
    }

    @Override
    public IStatus copy(IResource[] iResources, IPath iPath, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public IStatus copy(IResource[] iResources, IPath iPath, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public IStatus delete(IResource[] iResources, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public IStatus delete(IResource[] iResources, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public void deleteMarkers(IMarker[] iMarkers) throws CoreException {

    }

    @Override
    public void forgetSavedTree(String s) {

    }

    @Override
    public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
        return new IFilterMatcherDescriptor[0];
    }

    @Override
    public IFilterMatcherDescriptor getFilterMatcherDescriptor(String s) {
        return null;
    }

    @Override
    public IProjectNatureDescriptor[] getNatureDescriptors() {
        return new IProjectNatureDescriptor[0];
    }

    @Override
    public IProjectNatureDescriptor getNatureDescriptor(String s) {
        return null;
    }

    @Override
    public Map<IProject, IProject[]> getDanglingReferences() {
        return null;
    }

    public static WorkspaceDescription defaultWorkspaceDescription() {
        return new WorkspaceDescription("Workspace");
    }

    @Override
    public IWorkspaceDescription getDescription() {
        WorkspaceDescription workingCopy = defaultWorkspaceDescription();
        description.copyTo(workingCopy);
        return workingCopy;
    }

    @Override
    public IWorkspaceRoot getRoot() {
        return defaultRoot;
    }

    @Override
    public IResourceRuleFactory getRuleFactory() {
        if (this.ruleFactory == null) {
            this.ruleFactory = new ModelResourceRuleFactory(); //new Rules(this);
        }

        return this.ruleFactory;
    }

    @Override
    public ISynchronizer getSynchronizer() {
        return null;
    }

    @Override
    public boolean isAutoBuilding() {
        return false;
    }

    @Override
    public boolean isTreeLocked() {
        return false;
    }

    @Override
    public IProjectDescription loadProjectDescription(IPath iPath) throws CoreException {
        return null;
    }

    @Override
    public IProjectDescription loadProjectDescription(InputStream inputStream) throws CoreException {
        return null;
    }

    @Override
    public IStatus move(IResource[] iResources, IPath iPath, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public IStatus move(IResource[] iResources, IPath iPath, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public IBuildConfiguration newBuildConfig(String s, String s1) {
        return null;
    }

    @Override
    public IProjectDescription newProjectDescription(String s) {
        return null;
    }

    @Override
    public void removeResourceChangeListener(IResourceChangeListener iResourceChangeListener) {

    }

    @Override
    public void removeSaveParticipant(Plugin plugin) {

    }

    @Override
    public void removeSaveParticipant(String s) {

    }

    @Override
    public void run(ICoreRunnable action, ISchedulingRule rule, int options, IProgressMonitor monitor) throws CoreException {
        //TODO:
        action.run(monitor);
    }

    @Override
    public void run(ICoreRunnable action, IProgressMonitor monitor) throws CoreException {
        this.run(action, this.defaultRoot, 1, monitor);
    }

    @Override
    public void run(IWorkspaceRunnable action, ISchedulingRule rule, int options, IProgressMonitor monitor) throws CoreException {
        this.run((ICoreRunnable) action, rule, options, monitor);
    }

    @Override
    public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
        this.run((ICoreRunnable) action, this.defaultRoot, 1, monitor);
    }

    @Override
    public IStatus save(boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        return null;
    }

    @Override
    public void setDescription(IWorkspaceDescription value) throws CoreException {
        // if both the old and new description's build orders are null, leave the
        // workspace's build order slot because it is caching the computed order.
        // Otherwise, set the slot to null to force recomputing or building from the description.
        WorkspaceDescription newDescription = (WorkspaceDescription) value;
        //String[] newOrder = newDescription.getBuildOrder(false);
        //if (description.getBuildOrder(false) != null || newOrder != null) {
        //    flushBuildOrder();
        //}
        description.copyFrom(newDescription);
    }

    @Override
    public String[] sortNatureSet(String[] strings) {
        return new String[0];
    }

    //region ====validate methods====
    @Override
    public IStatus validateEdit(IFile[] iFiles, Object o) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validateFiltered(IResource iResource) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validateLinkLocation(IResource iResource, IPath iPath) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validateLinkLocationURI(IResource iResource, URI uri) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validateName(String s, int i) {
        //return locationValidator.validateName(segment, type);
        return Status.OK_STATUS; //TODO:检查名称有效性
    }

    @Override
    public IStatus validateNatureSet(String[] strings) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validatePath(String s, int i) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validateProjectLocation(IProject iProject, IPath iPath) {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus validateProjectLocationURI(IProject iProject, URI uri) {
        return Status.OK_STATUS;
    }
    //endregion

    @Override
    public IPathVariableManager getPathVariableManager() {
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> aClass) {
        return null;
    }

    protected long nextNodeId() {
        return nextNodeId++;
    }

    public void updateModificationStamp(ResourceInfo info) {
        info.incrementModificationStamp();
    }

    protected ResourceInfo newElement(int type) {
        ResourceInfo result = null;
        switch (type) {
            case IResource.FILE:
            case IResource.FOLDER:
                result = new ResourceInfo();
                break;
            case IResource.PROJECT:
                result = new ProjectInfo();
                break;
            case IResource.ROOT:
                result = new RootInfo();
                break;
        }
        result.setNodeId(nextNodeId());
        updateModificationStamp(result);
        result.setType(type);
        return result;
    }

    /**
     * Creates a resource, honoring update flags requesting that the resource
     * be immediately made derived, hidden and/or team private
     */
    public ResourceInfo createResource(IResource resource, int updateFlags) throws CoreException {
        ResourceInfo info = createResource(resource, null, false, false, false);
        if ((updateFlags & IResource.DERIVED) != 0)
            info.set(ICoreConstants.M_DERIVED);
        if ((updateFlags & IResource.TEAM_PRIVATE) != 0)
            info.set(ICoreConstants.M_TEAM_PRIVATE_MEMBER);
        if ((updateFlags & IResource.HIDDEN) != 0)
            info.set(ICoreConstants.M_HIDDEN);
        //		if ((updateFlags & IResource.VIRTUAL) != 0)
        //			info.set(M_VIRTUAL);
        return info;
    }

    /*
     * Creates the given resource in the tree and returns the new resource info object.
     * If phantom is true, the created element is marked as a phantom.
     * If there is already be an element in the tree for the given resource
     * in the given state (i.e., phantom), a CoreException is thrown.
     * If there is already a phantom in the tree and the phantom flag is false,
     * the element is overwritten with the new element. (but the synchronization
     * information is preserved) If the specified resource info is null, then create
     * a new one.
     *
     * If keepSyncInfo is set to be true, the sync info in the given ResourceInfo is NOT
     * cleared before being created and thus any sync info already existing at that namespace
     * (as indicated by an already existing phantom resource) will be lost.
     */
    public ResourceInfo createResource(IResource resource, ResourceInfo info, boolean phantom,
                                       boolean overwrite, boolean keepSyncInfo) throws CoreException {
        info = info == null ? newElement(resource.getType()) : (ResourceInfo) info.clone();
        ResourceInfo original = getResourceInfo(resource.getFullPath(), true, false);
        if (phantom) {
            info.set(ICoreConstants.M_PHANTOM);
            info.clearModificationStamp();
        }
        // if nothing existed at the destination then just create the resource in the tree
        if (original == null) {
            // we got here from a copy/move. we don't want to copy over any sync info
            // from the source so clear it.
            if (!keepSyncInfo) {
                //TODO:暂用反射
                //info.setSyncInfo(null);
                try {
                    ReflectUtil.setField(ResourceInfo.class, "syncInfo", info, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            tree.createElement(resource.getFullPath(), info);
        } else {
            // if overwrite==true then slam the new info into the tree even if one existed before
            if (overwrite || (!phantom && original.isSet(ICoreConstants.M_PHANTOM))) {
                // copy over the sync info and flags from the old resource info
                // since we are replacing a phantom with a real resource
                // DO NOT set the sync info dirty flag because we want to
                // preserve the old sync info so its not dirty
                // XXX: must copy over the generic sync info from the old info to the new
                // XXX: do we really need to clone the sync info here?
                if (!keepSyncInfo) {
                    //TODO:暂用反射
                    //info.setSyncInfo(original.getSyncInfo(true));
                    try {
                        ReflectUtil.setField(ResourceInfo.class, "syncInfo", info, original.getSyncInfo(true));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                // mark the markers bit as dirty so we snapshot an empty marker set for
                // the new resource
                info.set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
                tree.setElementData(resource.getFullPath(), info);
            } else {
                String message = Messages.resources_mustNotExist + ":" + resource.getFullPath();
                throw new ResourceException(IResourceStatus.RESOURCE_EXISTS, resource.getFullPath(), message, null);
            }
        }
        return info;
    }

    public ModelResource newResource(IPath path, int type) {
        String message;
        switch (type) {
            case IResource.FOLDER:
                if (path.segmentCount() < ICoreConstants.MINIMUM_FOLDER_SEGMENT_LENGTH) {
                    message = "Path must include project and resource name: " + path.toString();
                    Assert.isLegal(false, message);
                }
                return new ModelFolder(path.makeAbsolute(), this);
            case IResource.FILE:
                if (path.segmentCount() < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
                    message = "Path must include project and resource name: " + path.toString();
                    Assert.isLegal(false, message);
                }
                return new ModelFile(path.makeAbsolute(), this);
            case IResource.PROJECT:
                return (ModelResource) getRoot().getProject(path.lastSegment());
            case IResource.ROOT:
                return (ModelResource) getRoot();
        }
        Assert.isLegal(false);
        // will never get here because of assertion.
        return null;
    }

    /**
     * Returns the resource info for the identified resource.
     * null is returned if no such resource can be found.
     * If the phantom flag is true, phantom resources are considered.
     * If the mutable flag is true, the info is opened for change.
     * <p>
     * This method DOES NOT throw an exception if the resource is not found.
     */
    public ResourceInfo getResourceInfo(IPath path, boolean phantom, boolean mutable) {
        try {
            if (path.segmentCount() == 0) {
                ResourceInfo info = (ResourceInfo) tree.getTreeData();
                Assert.isNotNull(info, "Tree root info must never be null"); //$NON-NLS-1$
                return info;
            }
            ResourceInfo result = null;
            if (!tree.includes(path))
                return null;
            if (mutable)
                result = (ResourceInfo) tree.openElementData(path);
            else
                result = (ResourceInfo) tree.getElementData(path);
            if (result != null && (!phantom && result.isSet(ICoreConstants.M_PHANTOM)))
                return null;
            return result;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public long nextMarkerId() {
        return (long)(this.nextMarkerId++);
    }

    public ElementTree getElementTree() {
        return this.tree;
    }

    public LocalMetaArea getMetaArea() {
        return this.localMetaArea;
    }
}
