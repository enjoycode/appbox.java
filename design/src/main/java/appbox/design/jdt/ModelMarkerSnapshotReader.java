package appbox.design.jdt;

import org.eclipse.core.internal.resources.MarkerSnapshotReader;
import org.eclipse.core.internal.resources.MarkerSnapshotReader_1;
import org.eclipse.core.internal.resources.MarkerSnapshotReader_2;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import java.io.DataInputStream;
import java.io.IOException;

public class ModelMarkerSnapshotReader {

    protected ModelWorkspace workspace;

    public ModelMarkerSnapshotReader(ModelWorkspace workspace) {
        this.workspace = workspace;
    }

    protected ModelMarkerSnapshotReader getReader(int formatVersion) throws IOException {
        return this;
        //switch(formatVersion) {
        //    case 1:
        //        return new MarkerSnapshotReader_1(this.workspace);
        //    case 2:
        //        return new MarkerSnapshotReader_2(this.workspace);
        //    default:
        //        throw new IOException(NLS.bind(Messages.resources_format, formatVersion));
        //}
    }

    public void read(DataInputStream input) throws IOException, CoreException {
        int formatVersion = readVersionNumber(input);
        ModelMarkerSnapshotReader reader = this.getReader(formatVersion);
        reader.read(input);
    }

    protected static int readVersionNumber(DataInputStream input) throws IOException {
        return input.readInt();
    }
}
