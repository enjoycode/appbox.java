package appbox.design.lang.java.jdt;

import java.text.DateFormat;
import java.util.*;
import java.util.Map.Entry;

import appbox.logging.Log;
import org.eclipse.core.internal.resources.MarkerInfo;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class ModelMarker implements IMarker {

    /** Marker identifier. */
    protected long id;

    /** Resource with which this marker is associated. */
    protected IResource resource;

    /**
     * Constructs a new marker object.
     */
    protected ModelMarker(IResource resource, long id) {
        Assert.isLegal(resource != null);
        this.resource = resource;
        this.id       = id;
    }

    /**
     * Checks the given marker info to ensure that it is not null.
     * Throws an exception if it is.
     */
    private void checkInfo(MarkerInfo info) throws CoreException {
        if (info == null) {
            String message = Messages.resources_markerNotFound + ":" + Long.toString(id);
            throw new ResourceException(new ResourceStatus(IResourceStatus.MARKER_NOT_FOUND, resource.getFullPath(), message));
        }
    }

    @Override
    public void delete() throws CoreException {
        Log.warn("delete未实现");
        //final ISchedulingRule rule = getWorkspace().getRuleFactory().markerRule(resource);
        //try {
        //    getWorkspace().prepareOperation(rule, null);
        //    getWorkspace().beginOperation(true);
        //    getWorkspace().getMarkerManager().removeMarker(getResource(), getId());
        //} finally {
        //    getWorkspace().endOperation(rule, false);
        //}
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof IMarker))
            return false;
        IMarker other = (IMarker) object;
        return (id == other.getId() && resource.equals(other.getResource()));
    }

    @Override
    public boolean exists() {
        return getInfo() != null;
    }

    @Override
    public Object getAttribute(String attributeName) throws CoreException {
        Assert.isNotNull(attributeName);
        MarkerInfo info = getInfo();
        checkInfo(info);
        return info.getAttribute(attributeName);
    }

    @Override
    public int getAttribute(String attributeName, int defaultValue) {
        Assert.isNotNull(attributeName);
        MarkerInfo info = getInfo();
        if (info == null)
            return defaultValue;
        Object value = info.getAttribute(attributeName);
        if (value instanceof Integer)
            return ((Integer) value).intValue();
        return defaultValue;
    }

    @Override
    public String getAttribute(String attributeName, String defaultValue) {
        Assert.isNotNull(attributeName);
        MarkerInfo info = getInfo();
        if (info == null)
            return defaultValue;
        Object value = info.getAttribute(attributeName);
        if (value instanceof String)
            return (String) value;
        return defaultValue;
    }

    @Override
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        Assert.isNotNull(attributeName);
        MarkerInfo info = getInfo();
        if (info == null)
            return defaultValue;
        Object value = info.getAttribute(attributeName);
        if (value instanceof Boolean)
            return ((Boolean) value).booleanValue();
        return defaultValue;
    }

    @Override
    public Map<String, Object> getAttributes() throws CoreException {
        MarkerInfo info = getInfo();
        checkInfo(info);
        return info.getAttributes();
    }

    @Override
    public Object[] getAttributes(String[] attributeNames) throws CoreException {
        Assert.isNotNull(attributeNames);
        MarkerInfo info = getInfo();
        checkInfo(info);
        return info.getAttributes(attributeNames);
    }

    @Override
    public long getCreationTime() throws CoreException {
        MarkerInfo info = getInfo();
        checkInfo(info);
        return info.getCreationTime();
    }

    @Override
    public long getId() {
        return id;
    }

    protected MarkerInfo getInfo() {
        //Log.warn("未实现");
        return null;
        //return getWorkspace().getMarkerManager().findMarkerInfo(resource, id);
    }

    @Override
    public IResource getResource() {
        return resource;
    }

    @Override
    public String getType() throws CoreException {
        MarkerInfo info = getInfo();
        checkInfo(info);
        return info.getType();
    }

    /**
     * Returns the workspace which manages this marker.  Returns
     * <code>null</code> if this resource does not have an associated
     * resource.
     */
    private ModelWorkspace getWorkspace() {
        return resource == null ? null : (ModelWorkspace) resource.getWorkspace();
    }

    @Override
    public int hashCode() {
        return (int) id + resource.hashCode();
    }

    @Override
    public boolean isSubtypeOf(String type) throws CoreException {
        return false;
        //return getWorkspace().getMarkerManager().isSubtype(getType(), type);
    }

    @Override
    public void setAttribute(String attributeName, int value) throws CoreException {
        setAttribute(attributeName, Integer.valueOf(value));
    }

    @Override
    public void setAttribute(String attributeName, Object value) throws CoreException {
        Log.warn("未实现");
        //Assert.isNotNull(attributeName);
        //Workspace workspace = getWorkspace();
        //MarkerManager manager = workspace.getMarkerManager();
        //try {
        //    workspace.prepareOperation(null, null);
        //    workspace.beginOperation(true);
        //    MarkerInfo markerInfo = getInfo();
        //    checkInfo(markerInfo);
        //
        //    //only need to generate delta info if none already
        //    boolean needDelta = !manager.hasDelta(resource.getFullPath(), id);
        //    MarkerInfo oldInfo = needDelta ? (MarkerInfo) markerInfo.clone() : null;
        //    boolean validate = manager.isPersistentType(markerInfo.getType());
        //    markerInfo.setAttribute(attributeName, value, validate);
        //    if (manager.isPersistent(markerInfo))
        //        ((Resource) resource).getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
        //    if (needDelta) {
        //        MarkerDelta delta = new MarkerDelta(IResourceDelta.CHANGED, resource, oldInfo);
        //        manager.changedMarkers(resource, new MarkerDelta[] {delta});
        //    }
        //} finally {
        //    workspace.endOperation(null, false);
        //}
    }

    @Override
    public void setAttribute(String attributeName, boolean value) throws CoreException {
        setAttribute(attributeName, value ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public void setAttributes(String[] attributeNames, Object[] values) throws CoreException {
        if ((int) values[1] > 1) {
            Log.error("编译错误: [" + values[1] + " " + values[5] + "] " + values[0]);
        } else {
            Log.warn("编译警告: [" + values[1] + " " + values[5] + "] " + values[0]);
        }

        //Assert.isNotNull(attributeNames);
        //Assert.isNotNull(values);
        //Workspace workspace = getWorkspace();
        //MarkerManager manager = workspace.getMarkerManager();
        //try {
        //    workspace.prepareOperation(null, null);
        //    workspace.beginOperation(true);
        //    MarkerInfo markerInfo = getInfo();
        //    checkInfo(markerInfo);
        //
        //    //only need to generate delta info if none already
        //    boolean needDelta = !manager.hasDelta(resource.getFullPath(), id);
        //    MarkerInfo oldInfo = needDelta ? (MarkerInfo) markerInfo.clone() : null;
        //    boolean validate = manager.isPersistentType(markerInfo.getType());
        //    markerInfo.setAttributes(attributeNames, values, validate);
        //    if (manager.isPersistent(markerInfo))
        //        ((Resource) resource).getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
        //    if (needDelta) {
        //        MarkerDelta delta = new MarkerDelta(IResourceDelta.CHANGED, resource, oldInfo);
        //        manager.changedMarkers(resource, new MarkerDelta[] {delta});
        //    }
        //} finally {
        //    workspace.endOperation(null, false);
        //}
    }

    /**
     * @see IMarker#setAttributes(Map)
     */
    @Override
    public void setAttributes(Map<String, ? extends Object> values) throws CoreException {
        Log.warn("未实现");
        //Workspace workspace = getWorkspace();
        //MarkerManager manager = workspace.getMarkerManager();
        //try {
        //    workspace.prepareOperation(null, null);
        //    workspace.beginOperation(true);
        //    MarkerInfo markerInfo = getInfo();
        //    checkInfo(markerInfo);
        //
        //    //only need to generate delta info if none already
        //    boolean needDelta = !manager.hasDelta(resource.getFullPath(), id);
        //    MarkerInfo oldInfo = needDelta ? (MarkerInfo) markerInfo.clone() : null;
        //    boolean validate = manager.isPersistentType(markerInfo.getType());
        //    markerInfo.setAttributes(values, validate);
        //    if (manager.isPersistent(markerInfo))
        //        ((Resource) resource).getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
        //    if (needDelta) {
        //        MarkerDelta delta = new MarkerDelta(IResourceDelta.CHANGED, resource, oldInfo);
        //        manager.changedMarkers(resource, new MarkerDelta[] {delta});
        //    }
        //} finally {
        //    workspace.endOperation(null, false);
        //}
    }

    /** For debugging only */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Marker [");
        sb.append("on: ").append(resource.getFullPath());
        MarkerInfo info = getInfo();
        if (info == null) {
            sb.append(", not found]");
            return sb.toString();
        }
        sb.append(", id: ").append(info.getId());
        sb.append(", type: ").append(info.getType());
        Map<String, Object> attributes = info.getAttributes();
        if (attributes != null) {
            TreeMap<String, Object>    tm  = new TreeMap<>(attributes);
            Set<Entry<String, Object>> set = tm.entrySet();
            if (!set.isEmpty()) {
                sb.append(", attributes: [");
                for (Entry<String, Object> entry : set) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append(']');
            }
        }
        sb.append(", created: ").append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(info.getCreationTime())));
        sb.append(']');
        return sb.toString();
    }

    @Override
    public <T> T getAdapter(Class<T> aClass) {
        return null;
    }
}
