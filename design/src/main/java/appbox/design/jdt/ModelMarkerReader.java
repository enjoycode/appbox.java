package appbox.design.jdt;

import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import java.io.DataInputStream;
import java.io.IOException;

public class ModelMarkerReader {
    protected ModelWorkspace workspace;

    public ModelMarkerReader(ModelWorkspace workspace) {
        this.workspace = workspace;
    }

    protected ModelMarkerReader getReader(int formatVersion) throws IOException {
        return this;
        //switch(formatVersion) {
        //    case 1:
        //        return new ModelMarkerReader(this.workspace);
        //        //return new MarkerReader_1(this.workspace);
        //    case 2:
        //        return new ModelMarkerReader(this.workspace);
        //        //return new MarkerReader_2(this.workspace);
        //    case 3:
        //        return new ModelMarkerReader(this.workspace);
        //        //return new MarkerReader_3(this.workspace);
        //    default:
        //        throw new IOException(NLS.bind(Messages.resources_format, formatVersion));
        //}
    }

    public void read(DataInputStream input, boolean generateDeltas) throws IOException, CoreException {
        int formatVersion = readVersionNumber(input);
        ModelMarkerReader reader = this.getReader(formatVersion);
        reader.read(input, generateDeltas);
    }

    protected static int readVersionNumber(DataInputStream input) throws IOException {
        return input.readInt();
    }
}
