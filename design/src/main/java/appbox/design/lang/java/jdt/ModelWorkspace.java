package appbox.design.lang.java.jdt;

import appbox.design.utils.ReflectUtil;
import org.eclipse.core.internal.events.NotificationManager;
import org.eclipse.core.internal.properties.PropertyManager2;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.TeamHook;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public final class ModelWorkspace extends Workspace {

    private final HackWorkspaceDescription _description = new HackWorkspaceDescription();

    ModelWorkspace() {
        super();

        //hack defaultRoot
        final var hackDefaultRoot = new ModelWorkspaceRoot(new Path("/"), this);
        try {
            ReflectUtil.setField(Workspace.class, "defaultRoot", this, hackDefaultRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IStatus open(IProgressMonitor monitor) throws CoreException {
        //this.description =
        startup(monitor);
        this.openFlag = true;
        return Status.OK_STATUS;
    }

    @Override
    protected void startup(IProgressMonitor monitor) throws CoreException {
        try {
            this._workManager = new WorkManager(this);
            this._workManager.startup(null);
            //this.buildManager = new BuildManager(this, lock /*his._workManager.getLock()*/);
            //this.buildManager.startup(null);
            this.notificationManager = new NotificationManager(this);
            this.notificationManager.startup(null);
            this.markerManager = new MarkerManager(this);
            this.markerManager.startup(null);
            //this.saveManager = new SaveManager(this);
            //this.saveManager.startup(null);
            this.propertyManager = new PropertyManager2(this);
            this.propertyManager.startup(monitor);
        } finally {
            this.treeLocked = null;
            //this._workManager.postWorkspaceStartup();
            ReflectUtil.invokeMethod(WorkManager.class, "postWorkspaceStartup", this._workManager);
        }

    }

    @Override
    public Resource newResource(IPath path, int type) {
        switch (type) {
            case IResource.FOLDER:
                if (path.segmentCount() < ICoreConstants.MINIMUM_FOLDER_SEGMENT_LENGTH) {
                    throw new RuntimeException("Path must include project and resource name: " + path);
                }
                return new ModelFolder(path.makeAbsolute(), this);
            case IResource.FILE:
                if (path.segmentCount() < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
                    throw new RuntimeException("Path must include project and resource name: " + path);
                }
                return new ModelFile(path.makeAbsolute(), this);
            case IResource.PROJECT:
                return (Resource) getRoot().getProject(path.lastSegment());
            case IResource.ROOT:
                return (Resource) getRoot();
        }
        throw new RuntimeException();
    }

    @Override
    protected void initializeTeamHook() {
        this.teamHook = new TeamHook() {
        };
    }

    @Override
    public void prepareOperation(ISchedulingRule rule, IProgressMonitor monitor) throws CoreException {
        this.getWorkManager().checkIn(rule, monitor);

        if (!this.isOpen()) {
            String message = Messages.resources_workspaceClosed;
            throw new ResourceException(76, null, message, null);
        }
    }

    @Override
    public void endOperation(ISchedulingRule rule, boolean build) throws CoreException {
        WorkManager workManager = this.getWorkManager();
        if (!workManager.checkInFailed(rule)) {
            //boolean hasTreeChanges = false;
            boolean depthOne = false;

            try {
                workManager.setBuild(build);
                depthOne = workManager.getPreparedOperationDepth() == 1;
                if (!this.notificationManager.shouldNotify() && !depthOne) {
                    this.notificationManager.requestNotify();
                    return;
                }

                try {
                    //this.notificationManager.beginNotify();
                    Assert.isTrue(workManager.getPreparedOperationDepth() > 0, "Mismatched begin/endOperation");
                    workManager.rebalanceNestedOperations();
                    //hasTreeChanges = workManager.shouldBuild();
                    //if (hasTreeChanges) {
                    //    hasTreeChanges = this.operationTree != null && ElementTree.hasChanges(this.tree, this.operationTree, ResourceComparator.getBuildComparator(), true);
                    //}

                    //this.broadcastPostChange();
                    //this.saveManager.snapshotIfNeeded(hasTreeChanges);
                } finally {
                    if (depthOne) {
                        this.tree.immutable();
                        this.operationTree = null;
                    } else {
                        this.newWorkingTree();
                    }

                }
            } finally {
                workManager.checkOut(rule);
            }

            //if (depthOne) {
            //    this.buildManager.endTopLevel(hasTreeChanges);
            //}
        }
    }

    @Override
    public IWorkspaceDescription getDescription() {
        return _description;
    }

    //region ====Common Overrides for Resource====
    static void overrideResourceDelete(Resource resource, int updateFlags, IProgressMonitor monitor) throws CoreException {
        final var             workspace = (Workspace) resource.getWorkspace();
        final ISchedulingRule rule      = workspace.getRuleFactory().deleteRule(resource);
        try {
            workspace.prepareOperation(rule, monitor);
            // If there is no resource then there is nothing to delete so just return.
            if (!resource.exists())
                return;
            workspace.beginOperation(true);
            //broadcastPreDeleteEvent();

            // When a project is being deleted, flush the build order in case there is a problem.
            //if (resource.getType() == IResource.PROJECT)
            //    workspace.flushBuildOrder();

            final var message     = Messages.resources_deleteProblem;
            final var status      = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_DELETE_LOCAL, message, null);
            final var workManager = workspace.getWorkManager();
            final var lock        = (ILock) ReflectUtil.getField(WorkManager.class, "lock", workManager);
            final var tree        = new ModelResourceTree(lock, status, updateFlags);
            int       depth       = 0;
            try {
                depth = workManager.beginUnprotected();
                overrideUnprotectedDelete(resource, tree, updateFlags, monitor);
            } finally {
                workManager.endUnprotected(depth);
            }
            if (resource.getType() == IResource.ROOT) {
                // Need to clear out the root info.
                workspace.getMarkerManager().removeMarkers(resource, IResource.DEPTH_ZERO);
                workspace.getPropertyManager().deleteProperties(resource, IResource.DEPTH_ZERO);
                resource.getResourceInfo(false, false).clearSessionProperties();
            }
            // Invalidate the tree for further use by clients.
            tree.makeInvalid();
            if (!tree.getStatus().isOK())
                throw new ResourceException(tree.getStatus());

            // Update any aliases of this resource.
            // Note that deletion of a linked resource cannot affect other resources.
            //if (!wasLinked)
            //    workspace.getAliasManager().updateAliases(resource, originalStore, IResource.DEPTH_INFINITE, progress.split(48));
            if (resource.getType() == IResource.PROJECT) {
                // Make sure the rule factory is cleared on project deletion.
                //((Rules) workspace.getRuleFactory()).setRuleFactory((IProject) resource, null);
                final var ruleFactory = workspace.getRuleFactory();
                ReflectUtil.invokeMethod(ruleFactory.getClass(), "setRuleFactory",
                        IProject.class, IResourceRuleFactory.class, ruleFactory, resource, null);
                // Make sure project deletion is remembered.
                //workspace.getSaveManager().requestSnapshot();
            }
        } catch (OperationCanceledException e) {
            workspace.getWorkManager().operationCanceled();
            throw e;
        } finally {
            workspace.endOperation(rule, true);
        }
    }

    private static void overrideUnprotectedDelete(Resource resource, ModelResourceTree tree, int updateFlags, IProgressMonitor monitor) {
        switch (resource.getType()) {
            case IResource.FILE:
                tree.standardDeleteFile((IFile) resource, updateFlags, monitor);
                break;
            case IResource.FOLDER:
                tree.standardDeleteFolder((IFolder) resource, updateFlags, monitor);
                break;
            case IResource.PROJECT:
                tree.standardDeleteProject((IProject) resource, updateFlags, monitor);
                break;
            case IResource.ROOT:
                // When the root is deleted, all its children including hidden projects have to
                // be deleted.
                IProject[] projects = ((IWorkspaceRoot) resource).getProjects(IContainer.INCLUDE_HIDDEN);
                for (IProject project : projects) {
                    tree.standardDeleteProject(project, updateFlags, monitor);
                }
                break;
        }
    }

    /**
     * This method should be called to delete a resource from the tree because it will also
     * delete its properties and markers.  If a status object is provided, minor exceptions are
     * added, otherwise they are thrown.  If major exceptions occur, they are always thrown.
     */
    void overrideResourceDeleteResource(Resource resource, boolean convertToPhantom, MultiStatus status) throws CoreException {

        // Remove markers on this resource and its descendants.
        if (resource.exists())
            getMarkerManager().removeMarkers(resource, IResource.DEPTH_INFINITE);

        //// If this is a linked resource or contains linked resources,
        //// remove their entries from the project description.
        //List<Resource> links = findLinks();
        //// Pre-delete notification to internal infrastructure
        //if (links != null)
        //    for (Resource resource : links)
        //        workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_LINK_DELETE, resource));
        //
        //// Check if we deleted a preferences file.
        //ProjectPreferences.deleted(this);

        //// Remove all deleted linked resources from the project description.
        //if (getType() != IResource.PROJECT && links != null) {
        //    Project project = (Project) getProject();
        //    ProjectDescription description = project.internalGetDescription();
        //    if (description != null) {
        //        boolean wasChanged = false;
        //        for (Resource resource : links)
        //            wasChanged |= description.setLinkLocation(resource.getProjectRelativePath(), null);
        //        if (wasChanged) {
        //            project.internalSetDescription(description, true);
        //            try {
        //                project.writeDescription(IResource.FORCE);
        //            } catch (CoreException e) {
        //                // A problem happened updating the description, update the description in memory.
        //                project.updateDescription();
        //                throw e; // Rethrow.
        //            }
        //        }
        //    }
        //}

        // If we are synchronizing, do not delete the resource. Convert it
        // into a phantom. Actual deletion will happen when we refresh or push.
        //if (convertToPhantom && resource.getType() != IResource.PROJECT && synchronizing(getResourceInfo(true, false))) {
        //    convertToPhantom();
        //} else {
            deleteResource(resource);
        //}

        //List<Resource> filters = findFilters();
        //if ((filters != null) && (filters.size() > 0)) {
        //    // Delete resource filters.
        //    Project project = (Project) getProject();
        //    ProjectDescription description = project.internalGetDescription();
        //    if (description != null) {
        //        for (Resource resource : filters)
        //            description.setFilters(resource.getProjectRelativePath(), null);
        //        project.internalSetDescription(description, true);
        //        project.writeDescription(IResource.FORCE);
        //    }
        //}

        // Delete properties after the resource is deleted from the tree. See bug 84584.
        CoreException err = null;
        try {
            getPropertyManager().deleteResource(resource);
        } catch (CoreException e) {
            if (status != null) {
                status.add(e.getStatus());
            } else {
                err = e;
            }
        }
        if (err != null)
            throw err;
    }

    /**
     * Delete the given resource from the current tree of the receiver.
     * This method simply removes the resource from the tree.  No cleanup or
     * other management is done.  Use IResource.delete for proper deletion.
     * If the given resource is the root, all of its children (i.e., all projects) are
     * deleted but the root is left.
     */
    private void deleteResource(IResource resource) {
        IPath path = resource.getFullPath();
        if (path.equals(Path.ROOT)) {
            IProject[] children = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
            for (IProject element : children)
                tree.deleteElement(element.getFullPath());
        } else
            tree.deleteElement(path);
    }
    //endregion

}
