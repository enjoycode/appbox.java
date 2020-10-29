package appbox.design.jdt;

import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.JavaCore;

import java.net.URI;
import java.util.Map;

public class ModelProject extends ModelContainer implements IProject {

    public ModelProject(IPath path, ModelWorkspace workspace) {
        super(path, workspace);
    }

    @Override
    public void build(int i, String s, Map<String, String> map, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void build(int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void build(IBuildConfiguration iBuildConfiguration, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        create(description, IResource.NONE, monitor);
    }

    @Override
    public void create(IProgressMonitor monitor) throws CoreException {
        create(null, monitor);
    }

    @Override
    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.resources_create, 100);
        //checkValidPath(path, PROJECT, false);
        final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
        try {
            //workspace.prepareOperation(rule, subMonitor);
            if (description == null) {
                description = new ProjectDescription();
                description.setNatureIds(new String[]{JavaCore.NATURE_ID}); //Rick 直接设置
                description.setName(getName());
            }
            //assertCreateRequirements(description);
            //workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_PROJECT_CREATE, this));
            //workspace.beginOperation(true);
            workspace.createResource(this, updateFlags);
            //workspace.getMetaArea().create(this);
            ProjectInfo info = (ProjectInfo) getResourceInfo(false, true);

            // setup description to obtain project location
            ProjectDescription desc = (ProjectDescription) ((ProjectDescription) description).clone();
            //desc.setLocationURI(FileUtil.canonicalURI(description.getLocationURI()));
            desc.setName(getName());
            internalSetDescription(desc, false);
            //// see if there potentially are already contents on disk
            //final boolean hasSavedDescription = getLocalManager().hasSavedDescription(this);
            //boolean hasContent = hasSavedDescription;
            //// if there is no project description, there might still be content on disk
            //if (!hasSavedDescription)
            //    hasContent = getLocalManager().hasSavedContent(this);
            //try {
            //    // look for a description on disk
            //    if (hasSavedDescription) {
            //        updateDescription();
            //        // make sure the .location file is written
            //        workspace.getMetaArea().writePrivateDescription(this);
            //    } else {
            //        // write out the project
            //        writeDescription(IResource.FORCE);
            //    }
            //} catch (CoreException e) {
            //    workspace.deleteResource(this);
            //    throw e;
            //}
            // inaccessible projects have a null modification stamp.
            // set this after setting the description as #setDescription
            // updates the stamp
            info.clearModificationStamp();
            // if a project already had content on disk, mark the project as having unknown
            // children
            //if (hasContent)
            //    info.set(ICoreConstants.M_CHILDREN_UNKNOWN);
            //workspace.getSaveManager().requestSnapshot();
        } catch (OperationCanceledException e) {
            //workspace.getWorkManager().operationCanceled();
            throw e;
        } finally {
            subMonitor.done();
            //workspace.endOperation(rule, true);
        }
    }

    /**
     * Sets this project's description to the given value.  This is the body of the
     * corresponding API method but is needed separately since it is used
     * during workspace restore (i.e., when you cannot do an operation)
     */
    void internalSetDescription(IProjectDescription value, boolean incrementContentId) {
        //TODO:
        //// Project has been added / removed. Build order is out-of-step
        //workspace.flushBuildOrder();

        ProjectInfo info = (ProjectInfo) getResourceInfo(false, true);
        info.setDescription((ProjectDescription) value);
        //getLocalManager().setLocation(this, info, value.getLocationURI());
        if (incrementContentId) {
            info.incrementContentId();
            //if the project is not accessible, stamp will be null and should remain null
            if (info.getModificationStamp() != NULL_STAMP)
                workspace.updateModificationStamp(info);
        }
    }

    /**
     * This is an internal helper method. This implementation is different from the API
     * method getDescription(). This one does not check the project accessibility. It exists
     * in order to prevent "chicken and egg" problems in places like the project creation.
     * It may return null.
     */
    public ProjectDescription internalGetDescription() {
        ProjectInfo info = (ProjectInfo) getResourceInfo(false, false);
        if (info == null)
            return null;
        return info.getDescription();
    }

    @Override
    public void delete(boolean b, boolean b1, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
        return null;
    }

    @Override
    public IBuildConfiguration getBuildConfig(String s) throws CoreException {
        return null;
    }

    @Override
    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
        return new IBuildConfiguration[0];
    }

    @Override
    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        return null;
    }

    @Override
    public IProjectDescription getDescription() throws CoreException {
        ResourceInfo info = getResourceInfo(false, false);
        //checkAccessible(getFlags(info));
        ProjectDescription description = ((ProjectInfo) info).getDescription();
        //if the project is currently in the middle of being created, the description might not be available yet
        if (description == null)
            checkAccessible(ICoreConstants.NULL_FLAG);
        return (IProjectDescription) description.clone();
    }

    @Override
    public IContainer getParent() {
        return workspace.getRoot();
    }

    @Override
    public IProjectNature getNature(String s) throws CoreException {
        return null;
    }

    @Override
    public IPath getWorkingLocation(String s) {
        return null;
    }

    @Override
    public IProject[] getReferencedProjects() throws CoreException {
        return new IProject[0];
    }

    @Override
    public void clearCachedDynamicReferences() {

    }

    @Override
    public IProject[] getReferencingProjects() {
        return new IProject[0];
    }

    @Override
    public IBuildConfiguration[] getReferencedBuildConfigs(String s, boolean b) throws CoreException {
        return new IBuildConfiguration[0];
    }

    @Override
    public boolean hasBuildConfig(String s) throws CoreException {
        return false;
    }

    @Override
    public boolean hasNature(String natureID) throws CoreException {
        checkAccessible(getFlags(getResourceInfo(false, false)));
        // use #internal method to avoid copy but still throw an
        // exception if the resource doesn't exist.
        IProjectDescription desc = internalGetDescription();
        if (desc == null)
            checkAccessible(ICoreConstants.NULL_FLAG);
        return desc.hasNature(natureID);
    }

    @Override
    public boolean isNatureEnabled(String s) throws CoreException {
        return false;
    }

    @Override
    public boolean isOpen() {
        ResourceInfo info = getResourceInfo(false, false);
        return isOpen(getFlags(info));
    }

    public boolean isOpen(int flags) {
        //TODO:
        //return flags != ICoreConstants.NULL_FLAG && ResourceInfo.isSet(flags, ICoreConstants.M_OPEN);
        return flags != ICoreConstants.NULL_FLAG;
    }

    @Override
    public void loadSnapshot(int i, URI uri, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void move(IProjectDescription iProjectDescription, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
        //TODO:
        ProjectInfo info = (ProjectInfo) getResourceInfo(false, false);
        int flags = getFlags(info);
        checkExists(flags, true);
        if (isOpen(flags))
            return;

        info = (ProjectInfo) getResourceInfo(false, true);
        info.set(ICoreConstants.M_OPEN);
        //clear the unknown children immediately to avoid background refresh
        boolean unknownChildren = info.isSet(ICoreConstants.M_CHILDREN_UNKNOWN);
        if (unknownChildren)
            info.clear(ICoreConstants.M_CHILDREN_UNKNOWN);

        workspace.updateModificationStamp(info);
    }

    @Override
    public void open(IProgressMonitor monitor) throws CoreException {
        open(IResource.NONE, monitor);
    }

    @Override
    public void saveSnapshot(int i, URI uri, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void setDescription(IProjectDescription iProjectDescription, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public void setDescription(IProjectDescription iProjectDescription, int i, IProgressMonitor iProgressMonitor) throws CoreException {

    }

    @Override
    public IProject getProject() {
        return this;
    }

    @Override
    public int getType() {
        return IResource.PROJECT;
    }

    /**
     * Checks that this resource is accessible.  Typically this means that it
     * exists.  In the case of projects, they must also be open.
     * If phantom is true, phantom resources are considered.
     *
     * @exception CoreException if this resource is not accessible
     */
    @Override
    public void checkAccessible(int flags) throws CoreException {
        super.checkAccessible(flags);
        //if (!isOpen(flags)) {
        //    String message = Messages.resources_mustBeOpen + ":" + getName());
        //    throw new ResourceException(IResourceStatus.PROJECT_NOT_OPEN, getFullPath(), message, null);
        //}
    }
}
