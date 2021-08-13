package appbox.design.lang.java.jdt;

import appbox.design.DesignHub;
import appbox.design.utils.PathUtil;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.JavaCore;

public final class ModelProject extends Project {
    private ModelProjectType _projectType;
    private DesignHub        _designHub;

    ModelProject(IPath path, Workspace container) {
        super(path, container);
    }

    public void setProjectTypeAndDesignContext(ModelProjectType type, DesignHub hub) {
        _projectType = type;
        _designHub   = hub;
    }

    public ModelProjectType getProjectType() {return _projectType;}

    public DesignHub getDesignHub() {return _designHub;}

    @Override
    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        final var  workspace  = (Workspace) getWorkspace();
        SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.resources_create, 100);
        //checkValidPath(path, PROJECT, false);
        final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
        try {
            workspace.prepareOperation(rule, subMonitor);
            if (description == null) {
                description = new ProjectDescription();
                description.setNatureIds(new String[]{JavaCore.NATURE_ID}); //Rick 直接设置
                description.setName(getName());
            }
            //assertCreateRequirements(description);
            //workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_PROJECT_CREATE, this));
            workspace.beginOperation(true);
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
            workspace.getWorkManager().operationCanceled();
            throw e;
        } finally {
            subMonitor.done();
            workspace.endOperation(rule, true);
        }
    }

    @Override
    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
        final var workspace = (Workspace) getWorkspace();

        try {
            ISchedulingRule rule = workspace.getRuleFactory().modifyRule(this);

            try {
                workspace.prepareOperation(rule, monitor);
                ProjectInfo info  = (ProjectInfo) this.getResourceInfo(false, false);
                int         flags = this.getFlags(info);
                this.checkExists(flags, true);
                if (!this.isOpen(flags)) {
                    workspace.beginOperation(true);
                    //workspace.flushBuildOrder();
                    info = (ProjectInfo) this.getResourceInfo(false, true);
                    info.set(ICoreConstants.M_OPEN);
                    boolean unknownChildren = info.isSet(ICoreConstants.M_CHILDREN_UNKNOWN);
                    if (unknownChildren) {
                        info.clear(ICoreConstants.M_CHILDREN_UNKNOWN);
                    }

                    boolean used = info.isSet(ICoreConstants.M_USED);
                    //boolean snapshotLoaded = false;
                    //boolean minorIssuesDuringRestore;
                    //if (!used && !workspace.getMetaArea().getRefreshLocationFor(this).toFile().exists()) {
                    //    minorIssuesDuringRestore = this.getLocalManager().hasSavedDescription(this);
                    //    if (minorIssuesDuringRestore) {
                    //        ProjectDescription updatedDesc = info.getDescription();
                    //        if (updatedDesc != null) {
                    //            URI autoloadURI = updatedDesc.getSnapshotLocationURI();
                    //            if (autoloadURI != null) {
                    //                try {
                    //                    autoloadURI = this.getPathVariableManager().resolveURI(autoloadURI);
                    //                    this.internalLoadSnapshot(1, autoloadURI, Policy.subMonitorFor(monitor, Policy.opWork * 5 / 100));
                    //                    snapshotLoaded = true;
                    //                } catch (CoreException var25) {
                    //                    String msgerr = NLS.bind(Messages.projRead_cannotReadSnapshot, this.getName(), var25.getLocalizedMessage());
                    //                    Policy.log(new Status(2, "org.eclipse.core.resources", msgerr));
                    //                }
                    //            }
                    //        }
                    //    }
                    //}

                    //minorIssuesDuringRestore = false;
                    if (used) {
                        //    minorIssuesDuringRestore = workspace.getSaveManager().restore(this, Policy.subMonitorFor(monitor, Policy.opWork * 20 / 100));
                    } else {
                        info.set(ICoreConstants.M_USED);
                        //    IStatus result = this.reconcileLinksAndGroups(info.getDescription());
                        //    if (!result.isOK()) {
                        //        throw new CoreException(result);
                        //    }
                        //
                        workspace.updateModificationStamp(info);
                        //    monitor.worked(Policy.opWork * (snapshotLoaded ? 15 : 20) / 100);
                    }
                    //
                    //this.startup();
                    //if ((used || !unknownChildren) && minorIssuesDuringRestore) {
                    //    if ((updateFlags & 128) != 0) {
                    //        workspace.refreshManager.refresh(this);
                    //        monitor.worked(Policy.opWork * 60 / 100);
                    //    }
                    //} else {
                    //    boolean refreshed = false;
                    //    if (!used) {
                    //        refreshed = this.workspace.getSaveManager().restoreFromRefreshSnapshot(this, Policy.subMonitorFor(monitor, Policy.opWork * 20 / 100));
                    //        if (refreshed) {
                    //            monitor.worked(Policy.opWork * 60 / 100);
                    //        }
                    //    }
                    //
                    //    if (!refreshed) {
                    //        if ((updateFlags & 128) != 0) {
                    //            this.workspace.refreshManager.refresh(this);
                    //            monitor.worked(Policy.opWork * 60 / 100);
                    //        } else {
                    //            this.refreshLocal(2, Policy.subMonitorFor(monitor, Policy.opWork * 60 / 100));
                    //        }
                    //    }
                    //}
                    //
                    //workspace.getAliasManager().updateAliases(this, this.getStore(), 2, monitor);
                }
            } catch (OperationCanceledException ex) {
                workspace.getWorkManager().operationCanceled();
                throw ex;
            } finally {
                workspace.endOperation(rule, true);
            }
        } finally {
            if (monitor != null)
                monitor.done();
        }
    }

    /**
     * Sets this project's description to the given value.  This is the body of the
     * corresponding API method but is needed separately since it is used
     * during workspace restore (i.e., when you cannot do an operation)
     */
    void internalSetDescription(IProjectDescription value, boolean incrementContentId) {
        //TODO:check
        //// Project has been added / removed. Build order is out-of-step
        //workspace.flushBuildOrder();

        ProjectInfo info = (ProjectInfo) getResourceInfo(false, true);
        info.setDescription((ProjectDescription) value);
        //getLocalManager().setLocation(this, info, value.getLocationURI());
        if (incrementContentId) {
            info.incrementContentId();
            //if the project is not accessible, stamp will be null and should remain null
            if (info.getModificationStamp() != NULL_STAMP)
                ((Workspace) getWorkspace()).updateModificationStamp(info);
        }
    }

    @Override
    public String getDefaultCharset(boolean checkImplicit) {
        return "UTF8";
    }

    @Override
    public IPath getLocation() {
        return PathUtil.WORKSPACE_PATH.append(getFullPath());
    }

    @Override
    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return new IMarker[0]; //return super.findMarkers(type, includeSubtypes, depth);
    }

    public enum ModelProjectType {
        Models,
        DesigntimeService,
        RuntimeService,
        Test
    }
}
