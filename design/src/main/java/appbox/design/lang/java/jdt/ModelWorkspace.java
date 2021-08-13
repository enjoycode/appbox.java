package appbox.design.lang.java.jdt;

import appbox.design.utils.ReflectUtil;
import org.eclipse.core.internal.events.NotificationManager;
import org.eclipse.core.internal.properties.PropertyManager2;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.TeamHook;
import org.eclipse.core.runtime.*;
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
}
