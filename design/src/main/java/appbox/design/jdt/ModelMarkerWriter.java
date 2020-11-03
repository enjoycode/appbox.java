package appbox.design.jdt;

import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.watson.IPathRequestor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModelMarkerWriter {
    protected           ModelMarkerManager manager;
    public static final int           MARKERS_SAVE_VERSION = 3;
    public static final int           MARKERS_SNAP_VERSION = 2;
    public static final byte          INDEX = 1;
    public static final byte          QNAME = 2;
    public static final byte          ATTRIBUTE_NULL = 0;
    public static final byte          ATTRIBUTE_BOOLEAN = 1;
    public static final byte          ATTRIBUTE_INTEGER = 2;
    public static final byte          ATTRIBUTE_STRING = 3;

    public ModelMarkerWriter(ModelMarkerManager manager) {
        this.manager = manager;
    }

    private Object[] filterMarkers(IMarkerSetElement[] markers) {
        Object[] result = new Object[2];
        boolean[] isPersistent = new boolean[markers.length];
        int count = 0;

        for(int i = 0; i < markers.length; ++i) {
            MarkerInfo info = (MarkerInfo)markers[i];
            if (this.manager.isPersistent(info)) {
                isPersistent[i] = true;
                ++count;
            }
        }

        result[0] = count;
        result[1] = isPersistent;
        return result;
    }

    public void save(ResourceInfo info, IPathRequestor requestor, DataOutputStream output, List<String> writtenTypes) throws IOException {
        if (!info.isSet(8)) {
            MarkerSet markers = info.getMarkers(false);
            if (markers != null) {
                IMarkerSetElement[] elements = markers.elements();
                Object[] result = this.filterMarkers(elements);
                int count = (Integer)result[0];
                if (count != 0) {
                    if (output.size() == 0) {
                        output.writeInt(3);
                    }

                    boolean[] isPersistent = (boolean[])result[1];
                    output.writeUTF(requestor.requestPath().toString());
                    output.writeInt(count);

                    for(int i = 0; i < elements.length; ++i) {
                        if (isPersistent[i]) {
                            this.write((MarkerInfo)elements[i], output, writtenTypes);
                        }
                    }

                }
            }
        }
    }

    public void snap(ResourceInfo info, IPathRequestor requestor, DataOutputStream output) throws IOException {
        if (!info.isSet(8)) {
            if (info.isSet(4096)) {
                MarkerSet markers = info.getMarkers(false);
                if (markers != null) {
                    IMarkerSetElement[] elements = markers.elements();
                    Object[] result = this.filterMarkers(elements);
                    int count = (Integer)result[0];
                    output.writeInt(2);
                    boolean[] isPersistent = (boolean[])result[1];
                    output.writeUTF(requestor.requestPath().toString());
                    output.writeInt(count);
                    List<String> writtenTypes = new ArrayList();

                    for(int i = 0; i < elements.length; ++i) {
                        if (isPersistent[i]) {
                            this.write((MarkerInfo)elements[i], output, writtenTypes);
                        }
                    }

                    info.clear(4096);
                }
            }
        }
    }

    private void write(Map<String, Object> attributes, DataOutputStream output) throws IOException {
        output.writeShort(attributes.size());
        Iterator var4 = attributes.entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<String, Object> e   = (Map.Entry)var4.next();
            String                    key = (String)e.getKey();
            output.writeUTF(key);
            Object value = e.getValue();
            if (value instanceof Integer) {
                output.writeByte(2);
                output.writeInt((Integer)value);
            } else if (value instanceof Boolean) {
                output.writeByte(1);
                output.writeBoolean((Boolean)value);
            } else if (value instanceof String) {
                output.writeByte(3);
                output.writeUTF((String)value);
            } else {
                output.writeByte(0);
            }
        }

    }

    private void write(MarkerInfo info, DataOutputStream output, List<String> writtenTypes) throws IOException {
        output.writeLong(info.getId());
        String type = info.getType();
        int index = writtenTypes.indexOf(type);
        if (index == -1) {
            output.writeByte(2);
            output.writeUTF(type);
            writtenTypes.add(type);
        } else {
            output.writeByte(1);
            output.writeInt(index);
        }

        if (info.getAttributes(false) == null) {
            output.writeShort(0);
        } else {
            this.write(info.getAttributes(false), output);
        }

        output.writeLong(info.getCreationTime());
    }
}
