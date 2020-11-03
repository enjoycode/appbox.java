package appbox.design.jdt;

import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.internal.localstore.SafeFileInputStream;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeIterator;
import org.eclipse.core.internal.watson.IElementContentVisitor;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import java.io.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelMarkerManager implements IManager {
    private static final MarkerInfo[]              NO_MARKER_INFO = new MarkerInfo[0];
    private static final IMarker[]                 NO_MARKERS     = new IMarker[0];
    protected            MarkerTypeDefinitionCache cache          = new MarkerTypeDefinitionCache();
    private              long                  changeId      = 0L;
    protected            Map<IPath, MarkerSet> currentDeltas = null;
    protected final      ModelMarkerDeltaManager    deltaManager  = new ModelMarkerDeltaManager();
    protected            ModelWorkspace                 workspace;
    //protected            ModelMarkerWriter              writer         = new ModelMarkerWriter(this);

    public ModelMarkerManager(ModelWorkspace workspace) {
        this.workspace = workspace;
    }

    public void add(IResource resource, MarkerInfo newMarker) throws CoreException {
        Resource target = (Resource)resource;
        ResourceInfo info = this.workspace.getResourceInfo(target.getFullPath(), false, false);
        target.checkExists(target.getFlags(info), false);
        info = this.workspace.getResourceInfo(resource.getFullPath(), false, true);
        if (info != null) {
            if (this.isPersistent(newMarker)) {
                info.set(4096);
            }

            MarkerSet markers = info.getMarkers(true);
            if (markers == null) {
                markers = new MarkerSet(1);
            }

            this.basicAdd(resource, markers, newMarker);
            if (!markers.isEmpty()) {
                info.setMarkers(markers);
            }

        }
    }

    private void basicAdd(IResource resource, MarkerSet markers, MarkerInfo newMarker) throws CoreException {
        if (newMarker.getId() != -1L) {
            String message = Messages.resources_changeInAdd;
            throw new ResourceException(new ResourceStatus(566, resource.getFullPath(), message));
        } else {
            newMarker.setId(this.workspace.nextMarkerId());
            markers.add(newMarker);
            IMarkerSetElement[] changes = new IMarkerSetElement[]{new MarkerDelta(1, resource, newMarker)};
            this.changedMarkers(resource, changes);
        }
    }

    protected MarkerInfo[] basicFindMatching(MarkerSet markers, String type, boolean includeSubtypes) {
        int size = markers.size();
        if (size <= 0) {
            return NO_MARKER_INFO;
        } else {
            List<MarkerInfo>    result   = new ArrayList(size);
            IMarkerSetElement[] elements = markers.elements();
            IMarkerSetElement[] var10 = elements;
            int var9 = elements.length;

            for(int var8 = 0; var8 < var9; ++var8) {
                IMarkerSetElement element = var10[var8];
                MarkerInfo marker = (MarkerInfo)element;
                if (type == null) {
                    result.add(marker);
                } else if (includeSubtypes) {
                    if (this.cache.isSubtype(marker.getType(), type)) {
                        result.add(marker);
                    }
                } else if (marker.getType().equals(type)) {
                    result.add(marker);
                }
            }

            size = result.size();
            if (size <= 0) {
                return NO_MARKER_INFO;
            } else {
                return (MarkerInfo[])result.toArray(new MarkerInfo[size]);
            }
        }
    }

    protected int basicFindMaxSeverity(MarkerSet markers, String type, boolean includeSubtypes) {
        int max = -1;
        int size = markers.size();
        if (size <= 0) {
            return max;
        } else {
            IMarkerSetElement[] elements = markers.elements();
            IMarkerSetElement[] var10 = elements;
            int var9 = elements.length;

            for(int var8 = 0; var8 < var9; ++var8) {
                IMarkerSetElement element = var10[var8];
                MarkerInfo marker = (MarkerInfo)element;
                if (type == null) {
                    max = Math.max(max, this.getSeverity(marker));
                } else if (includeSubtypes) {
                    if (this.cache.isSubtype(marker.getType(), type)) {
                        max = Math.max(max, this.getSeverity(marker));
                    }
                } else if (marker.getType().equals(type)) {
                    max = Math.max(max, this.getSeverity(marker));
                }

                if (max >= 2) {
                    break;
                }
            }

            return max;
        }
    }

    private int getSeverity(MarkerInfo marker) {
        Object o = marker.getAttribute("severity");
        if (o instanceof Integer) {
            Integer i = (Integer)o;
            return i;
        } else {
            return -1;
        }
    }

    protected void basicRemoveMarkers(ResourceInfo info, IPathRequestor requestor, String type, boolean includeSubtypes) {
        MarkerSet markers = info.getMarkers(false);
        if (markers != null) {
            Object matching;
            IPath path;
            if (type == null) {
                path = requestor.requestPath();
                info = this.workspace.getResourceInfo(path, false, true);
                info.setMarkers((MarkerSet)null);
                matching = markers.elements();
            } else {
                matching = this.basicFindMatching(markers, type, includeSubtypes);
                if (((Object[])matching).length == 0) {
                    return;
                }

                path = requestor.requestPath();
                info = this.workspace.getResourceInfo(path, false, true);
                markers = info.getMarkers(true);
                if (markers.size() == ((Object[])matching).length) {
                    info.setMarkers((MarkerSet)null);
                } else {
                    markers.removeAll((IMarkerSetElement[])matching);
                    info.setMarkers(markers);
                }
            }

            info.set(4096);
            IMarkerSetElement[] changes = new IMarkerSetElement[((Object[])matching).length];
            IResource resource = this.workspace.getRoot().findMember(path);

            for(int i = 0; i < ((Object[])matching).length; ++i) {
                changes[i] = new MarkerDelta(2, resource, (MarkerInfo)((Object[])matching)[i]);
            }

            this.changedMarkers(resource, changes);
        }
    }

    protected void buildMarkers(IMarkerSetElement[] markers, IPath path, int type, ArrayList<IMarker> list) {
        if (markers.length != 0) {
            IResource resource = this.workspace.newResource(path, type);
            list.ensureCapacity(list.size() + markers.length);
            IMarkerSetElement[] var9 = markers;
            int var8 = markers.length;

            for(int var7 = 0; var7 < var8; ++var7) {
                IMarkerSetElement marker = var9[var7];
                list.add(new ModelMarker(resource, ((MarkerInfo)marker).getId()));
            }

        }
    }

    protected void changedMarkers(IResource resource, IMarkerSetElement[] changes) {
        if (changes != null && changes.length != 0) {
            ++this.changeId;
            if (this.currentDeltas == null) {
                this.currentDeltas = this.deltaManager.newGeneration(this.changeId);
            }

            IPath path = resource.getFullPath();
            MarkerSet previousChanges = (MarkerSet)this.currentDeltas.get(path);
            MarkerSet result = ModelMarkerDelta.merge(previousChanges, changes);
            if (result.size() == 0) {
                this.currentDeltas.remove(path);
            } else {
                this.currentDeltas.put(path, result);
            }

            ResourceInfo info = this.workspace.getResourceInfo(path, false, true);
            if (info != null) {
                info.incrementMarkerGenerationCount();
            }

        }
    }

    public IMarker findMarker(IResource resource, long id) {
        MarkerInfo info = this.findMarkerInfo(resource, id);
        return info == null ? null : new ModelMarker(resource, info.getId());
    }

    public MarkerInfo findMarkerInfo(IResource resource, long id) {
        ResourceInfo info = this.workspace.getResourceInfo(resource.getFullPath(), false, false);
        if (info == null) {
            return null;
        } else {
            MarkerSet markers = info.getMarkers(false);
            return markers == null ? null : (MarkerInfo)markers.get(id);
        }
    }

    public IMarker[] findMarkers(IResource target, String type, boolean includeSubtypes, int depth) {
        ArrayList<IMarker> result = new ArrayList();
        this.doFindMarkers(target, result, type, includeSubtypes, depth);
        return result.isEmpty() ? NO_MARKERS : (IMarker[])result.toArray(new IMarker[result.size()]);
    }

    public void doFindMarkers(IResource target, ArrayList<IMarker> result, String type, boolean includeSubtypes, int depth) {
        if (depth == 2 && target.getType() != 1) {
            this.visitorFindMarkers(target.getFullPath(), result, type, includeSubtypes);
        } else {
            this.recursiveFindMarkers(target.getFullPath(), result, type, includeSubtypes, depth);
        }

    }

    public int findMaxProblemSeverity(IResource target, String type, boolean includeSubtypes, int depth) {
        return depth == 2 && target.getType() != 1 ? this.visitorFindMaxSeverity(target.getFullPath(), type, includeSubtypes) : this.recursiveFindMaxSeverity(target.getFullPath(), type, includeSubtypes, depth);
    }

    public long getChangeId() {
        return this.changeId;
    }

    public Map<IPath, MarkerSet> getMarkerDeltas(long startChangeId) {
        return this.deltaManager.assembleDeltas(startChangeId);
    }

    boolean hasDelta(IPath path, long id) {
        if (this.currentDeltas == null) {
            return false;
        } else {
            MarkerSet set = (MarkerSet)this.currentDeltas.get(path);
            if (set == null) {
                return false;
            } else {
                return set.get(id) != null;
            }
        }
    }

    public boolean isPersistent(MarkerInfo info) {
        if (!this.cache.isPersistent(info.getType())) {
            return false;
        } else {
            Object isTransient = info.getAttribute("transient");
            return isTransient == null || !(isTransient instanceof Boolean) || !(Boolean)isTransient;
        }
    }

    public boolean isPersistentType(String type) {
        return this.cache.isPersistent(type);
    }

    public boolean isSubtype(String type, String superType) {
        return this.cache.isSubtype(type, superType);
    }

    public void moved(IResource source, IResource destination, int depth) throws CoreException {
        int count = destination.getFullPath().segmentCount();
        IResourceVisitor visitor = (resource) -> {
            Resource r = (Resource)resource;
            ResourceInfo info = r.getResourceInfo(false, true);
            MarkerSet markers = info.getMarkers(false);
            if (markers == null) {
                return true;
            } else {
                info.set(4096);
                IMarkerSetElement[] removed = new IMarkerSetElement[markers.size()];
                IMarkerSetElement[] added = new IMarkerSetElement[markers.size()];
                IPath path = resource.getFullPath().removeFirstSegments(count);
                path = source.getFullPath().append(path);
                IResource sourceChild = this.workspace.newResource(path, resource.getType());
                IMarkerSetElement[] elements = markers.elements();

                for(int i = 0; i < elements.length; ++i) {
                    MarkerInfo markerInfo = (MarkerInfo)elements[i];
                    MarkerDelta delta = new MarkerDelta(1, resource, markerInfo);
                    added[i] = delta;
                               delta = new MarkerDelta(2, sourceChild, markerInfo);
                    removed[i] = delta;
                }

                this.changedMarkers(resource, added);
                this.changedMarkers(sourceChild, removed);
                return true;
            }
        };
        destination.accept(visitor, depth, 10);
    }

    private void recursiveFindMarkers(IPath path, ArrayList<IMarker> list, String type, boolean includeSubtypes, int depth) {
        ResourceInfo info = this.workspace.getResourceInfo(path, false, false);
        if (info != null) {
            MarkerSet markers = info.getMarkers(false);
            if (markers != null) {
                Object matching;
                if (type == null) {
                    matching = markers.elements();
                } else {
                    matching = this.basicFindMatching(markers, type, includeSubtypes);
                }

                this.buildMarkers((IMarkerSetElement[])matching, path, info.getType(), list);
            }

            if (depth != 0 && info.getType() != 1) {
                if (depth == 1) {
                    depth = 0;
                }

                IPath[] var11;
                int var10 = (var11 = this.workspace.getElementTree().getChildren(path)).length;

                for(int var9 = 0; var9 < var10; ++var9) {
                    IPath child = var11[var9];
                    this.recursiveFindMarkers(child, list, type, includeSubtypes, depth);
                }

            }
        }
    }

    private int recursiveFindMaxSeverity(IPath path, String type, boolean includeSubtypes, int depth) {
        ResourceInfo info = this.workspace.getResourceInfo(path, false, false);
        if (info == null) {
            return -1;
        } else {
            MarkerSet markers = info.getMarkers(false);
            int max = -1;
            if (markers != null) {
                max = this.basicFindMaxSeverity(markers, type, includeSubtypes);
                if (max >= 2) {
                    return max;
                }
            }

            if (depth != 0 && info.getType() != 1) {
                if (depth == 1) {
                    depth = 0;
                }

                IPath[] var11;
                int var10 = (var11 = this.workspace.getElementTree().getChildren(path)).length;

                for(int var9 = 0; var9 < var10; ++var9) {
                    IPath child = var11[var9];
                    max = Math.max(max, this.recursiveFindMaxSeverity(child, type, includeSubtypes, depth));
                    if (max >= 2) {
                        break;
                    }
                }

                return max;
            } else {
                return max;
            }
        }
    }

    private void recursiveRemoveMarkers(final IPath path, String type, boolean includeSubtypes, int depth) {
        ResourceInfo info = this.workspace.getResourceInfo(path, false, false);
        if (info != null) {
            IPathRequestor requestor = new IPathRequestor() {
                public String requestName() {
                    return path.lastSegment();
                }

                public IPath requestPath() {
                    return path;
                }
            };
            this.basicRemoveMarkers(info, requestor, type, includeSubtypes);
            if (depth != 0 && info.getType() != 1) {
                if (depth == 1) {
                    depth = 0;
                }

                IPath[] var10;
                int var9 = (var10 = this.workspace.getElementTree().getChildren(path)).length;

                for(int var8 = 0; var8 < var9; ++var8) {
                    IPath child = var10[var8];
                    this.recursiveRemoveMarkers(child, type, includeSubtypes, depth);
                }

            }
        }
    }

    public void removeMarker(IResource resource, long id) {
        MarkerInfo markerInfo = this.findMarkerInfo(resource, id);
        if (markerInfo != null) {
            ResourceInfo info = ((Workspace)resource.getWorkspace()).getResourceInfo(resource.getFullPath(), false, true);
            MarkerSet markers = info.getMarkers(true);
            int size = markers.size();
            markers.remove(markerInfo);
            info.setMarkers(markers.size() == 0 ? null : markers);
            if (markers.size() != size) {
                if (this.isPersistent(markerInfo)) {
                    info.set(4096);
                }

                IMarkerSetElement[] change = new IMarkerSetElement[]{new MarkerDelta(2, resource, markerInfo)};
                this.changedMarkers(resource, change);
            }

        }
    }

    public void removeMarkers(IResource resource, int depth) {
        this.removeMarkers(resource, (String)null, false, depth);
    }

    public void removeMarkers(IResource target, String type, boolean includeSubtypes, int depth) {
        if (depth == 2 && target.getType() != 1) {
            this.visitorRemoveMarkers(target.getFullPath(), type, includeSubtypes);
        } else {
            this.recursiveRemoveMarkers(target.getFullPath(), type, includeSubtypes, depth);
        }

    }

    public void resetMarkerDeltas(long startId) {
        this.currentDeltas = null;
        this.deltaManager.resetDeltas(startId);
    }

    //public void restore(IResource resource, boolean generateDeltas, IProgressMonitor monitor) throws Throwable {
    //    this.restoreFromSave(resource, generateDeltas);
    //    this.restoreFromSnap(resource);
    //}

    //protected void restoreFromSave(IResource resource, boolean generateDeltas) throws Throwable {
    //    IPath sourceLocation = this.workspace.getMetaArea().getMarkersLocationFor(resource);
    //    IPath        tempLocation = this.workspace.getMetaArea().getBackupLocationFor(sourceLocation);
    //    java.io.File sourceFile   = new java.io.File(sourceLocation.toOSString());
    //    java.io.File tempFile     = new File(tempLocation.toOSString());
    //    if (sourceFile.exists() || tempFile.exists()) {
    //        String msg;
    //        try {
    //            Throwable var7 = null;
    //            msg = null;
    //
    //            try {
    //                DataInputStream input = new DataInputStream(new SafeFileInputStream(sourceLocation.toOSString(), tempLocation.toOSString()));
    //
    //                try {
    //                    ModelMarkerReader reader = new ModelMarkerReader(this.workspace);
    //                    reader.read(input, generateDeltas);
    //                } finally {
    //                    if (input != null) {
    //                        input.close();
    //                    }
    //
    //                }
    //
    //            } catch (Throwable var18) {
    //                if (var7 == null) {
    //                    var7 = var18;
    //                } else if (var7 != var18) {
    //                    var7.addSuppressed(var18);
    //                }
    //
    //                throw var7;
    //            }
    //        } catch (Exception var19) {
    //            msg = NLS.bind(Messages.resources_readMeta, sourceLocation);
    //            throw new ResourceException(567, sourceLocation, msg, var19);
    //        }
    //    }
    //}

    //protected void restoreFromSnap(IResource resource) throws Throwable {
    //    IPath sourceLocation = this.workspace.getMetaArea().getMarkersSnapshotLocationFor(resource);
    //    if (sourceLocation.toFile().exists()) {
    //        String msg;
    //        try {
    //            Throwable var3 = null;
    //            msg = null;
    //
    //            try {
    //                DataInputStream input = new DataInputStream(new SafeChunkyInputStream(sourceLocation.toFile()));
    //
    //                try {
    //                    ModelMarkerSnapshotReader reader = new ModelMarkerSnapshotReader(this.workspace);
    //
    //                    while(true) {
    //                        reader.read(input);
    //                    }
    //                } finally {
    //                    if (input != null) {
    //                        input.close();
    //                    }
    //
    //                }
    //            } catch (Throwable var16) {
    //                if (var3 == null) {
    //                    var3 = var16;
    //                } else if (var3 != var16) {
    //                    var3.addSuppressed(var16);
    //                }
    //
    //                throw var3;
    //            }
    //        } catch (EOFException var17) {
    //        } catch (Exception var18) {
    //            msg = NLS.bind(Messages.resources_readMeta, sourceLocation);
    //            Policy.log(new ResourceStatus(567, sourceLocation, msg, var18));
    //        }
    //
    //    }
    //}

    //public void save(ResourceInfo info, IPathRequestor requestor, DataOutputStream output, List<String> list) throws IOException {
    //    this.writer.save(info, requestor, output, list);
    //}

    public void shutdown(IProgressMonitor monitor) {
    }

    //public void snap(ResourceInfo info, IPathRequestor requestor, DataOutputStream output) throws IOException {
    //    this.writer.snap(info, requestor, output);
    //}

    public void startup(IProgressMonitor monitor) {
    }

    private void visitorFindMarkers(IPath path, ArrayList<IMarker> list, String type, boolean includeSubtypes) {
        IElementContentVisitor visitor = (tree, requestor, elementContents) -> {
            ResourceInfo info = (ResourceInfo)elementContents;
            if (info == null) {
                return false;
            } else {
                MarkerSet markers = info.getMarkers(false);
                if (markers != null) {
                    Object matching;
                    if (type == null) {
                        matching = markers.elements();
                    } else {
                        matching = this.basicFindMatching(markers, type, includeSubtypes);
                    }

                    this.buildMarkers((IMarkerSetElement[])matching, requestor.requestPath(), info.getType(), list);
                }

                return true;
            }
        };
        (new ElementTreeIterator(this.workspace.getElementTree(), path)).iterate(visitor);
    }

    private int visitorFindMaxSeverity(IPath path, final String type, final boolean includeSubtypes) {
        class MaxSeverityVisitor implements IElementContentVisitor {
            int max = -1;

            MaxSeverityVisitor() {
            }

            public boolean visitElement(ElementTree tree, IPathRequestor requestor, Object elementContents) {
                if (this.max >= 2) {
                    return false;
                } else {
                    ResourceInfo info = (ResourceInfo)elementContents;
                    if (info == null) {
                        return false;
                    } else {
                        MarkerSet markers = info.getMarkers(false);
                        if (markers != null) {
                            this.max = Math.max(this.max, ModelMarkerManager.this.basicFindMaxSeverity(markers, type, includeSubtypes));
                        }

                        return this.max < 2;
                    }
                }
            }
        }

        MaxSeverityVisitor visitor = new MaxSeverityVisitor();
        (new ElementTreeIterator(this.workspace.getElementTree(), path)).iterate(visitor);
        return visitor.max;
    }

    private void visitorRemoveMarkers(IPath path, String type, boolean includeSubtypes) {
        IElementContentVisitor visitor = (tree, requestor, elementContents) -> {
            ResourceInfo info = (ResourceInfo)elementContents;
            if (info == null) {
                return false;
            } else {
                this.basicRemoveMarkers(info, requestor, type, includeSubtypes);
                return true;
            }
        };
        (new ElementTreeIterator(this.workspace.getElementTree(), path)).iterate(visitor);
    }


}
