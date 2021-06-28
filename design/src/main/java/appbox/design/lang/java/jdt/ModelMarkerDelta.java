package appbox.design.lang.java.jdt;

import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import java.util.Iterator;
import java.util.Map;

public class ModelMarkerDelta implements IMarkerDelta, IMarkerSetElement{
    protected int        kind;
    protected IResource  resource;
    protected MarkerInfo info;

    public ModelMarkerDelta(int kind, IResource resource, MarkerInfo info) {
        this.kind = kind;
        this.resource = resource;
        this.info = info;
    }

    public Object getAttribute(String attributeName) {
        return this.info.getAttribute(attributeName);
    }

    public int getAttribute(String attributeName, int defaultValue) {
        Object value = this.info.getAttribute(attributeName);
        return value instanceof Integer ? (Integer)value : defaultValue;
    }

    public String getAttribute(String attributeName, String defaultValue) {
        Object value = this.info.getAttribute(attributeName);
        return value instanceof String ? (String)value : defaultValue;
    }

    public boolean getAttribute(String attributeName, boolean defaultValue) {
        Object value = this.info.getAttribute(attributeName);
        return value instanceof Boolean ? (Boolean)value : defaultValue;
    }

    public Map<String, Object> getAttributes() {
        return this.info.getAttributes();
    }

    public Object[] getAttributes(String[] attributeNames) {
        return this.info.getAttributes(attributeNames);
    }

    public long getId() {
        return this.info.getId();
    }

    public int getKind() {
        return this.kind;
    }

    public IMarker getMarker() {
        return new ModelMarker(this.resource, this.getId());
    }

    public IResource getResource() {
        return this.resource;
    }

    public String getType() {
        return this.info.getType();
    }

    public boolean isSubtypeOf(String superType) {
        return ((Workspace)this.getResource().getWorkspace()).getMarkerManager().isSubtype(this.getType(), superType);
    }

    public static Map<IPath, MarkerSet> merge(Map<IPath, MarkerSet> oldChanges, Map<IPath, MarkerSet> newChanges) {
        if (oldChanges == null) {
            return newChanges;
        } else if (newChanges == null) {
            return oldChanges;
        } else {
            Iterator var3 = newChanges.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry<IPath, MarkerSet> newEntry = (Map.Entry)var3.next();
                IPath                       key      = (IPath)newEntry.getKey();
                MarkerSet oldSet = (MarkerSet)oldChanges.get(key);
                MarkerSet newSet = (MarkerSet)newEntry.getValue();
                if (oldSet == null) {
                    oldChanges.put(key, newSet);
                } else {
                    merge(oldSet, newSet.elements());
                }
            }

            return oldChanges;
        }
    }

    protected static MarkerSet merge(MarkerSet oldChanges, IMarkerSetElement[] newChanges) {
        int var4;
        if (oldChanges != null) {
            if (newChanges == null) {
                return oldChanges;
            } else {
                IMarkerSetElement[] var10 = newChanges;
                var4 = newChanges.length;

                for(int var9 = 0; var9 < var4; ++var9) {
                    IMarkerSetElement newChange = var10[var9];
                    ModelMarkerDelta newDelta = (ModelMarkerDelta)newChange;
                    ModelMarkerDelta oldDelta = (ModelMarkerDelta)oldChanges.get(newDelta.getId());
                    if (oldDelta == null) {
                        oldChanges.add(newDelta);
                    } else {
                        switch(oldDelta.getKind()) {
                            case 1:
                                switch(newDelta.getKind()) {
                                    case 1:
                                    case 3:
                                    case 4:
                                    default:
                                        continue;
                                    case 2:
                                        oldChanges.remove(oldDelta);
                                        continue;
                                }
                            case 2:
                                switch(newDelta.getKind()) {
                                    case 1:
                                    case 2:
                                    case 3:
                                    case 4:
                                }
                            case 3:
                            default:
                                break;
                            case 4:
                                switch(newDelta.getKind()) {
                                    case 1:
                                    case 3:
                                    case 4:
                                    default:
                                        break;
                                    case 2:
                                        oldDelta.setKind(2);
                                }
                        }
                    }
                }

                return oldChanges;
            }
        } else {
            MarkerSet result = new MarkerSet(newChanges.length);
            IMarkerSetElement[] var6 = newChanges;
            int var5 = newChanges.length;

            for(var4 = 0; var4 < var5; ++var4) {
                IMarkerSetElement newChange = var6[var4];
                result.add(newChange);
            }

            return result;
        }
    }

    private void setKind(int kind) {
        this.kind = kind;
    }
}
