package appbox.data;

import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

import java.time.LocalDateTime;

public final class BlobObject implements IJsonSerializable {
    public final String        name;
    public final long          size;
    public final LocalDateTime modifiedTime;
    public final boolean       isFile;

    public BlobObject(String name, long size, LocalDateTime modifiedTime, boolean isFile) {
        this.name         = name;
        this.size         = size;
        this.modifiedTime = modifiedTime;
        this.isFile       = isFile;
    }

    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startObject();
        writer.writeKeyValue("Name", name);
        writer.writeKeyValue("Size", size);
        writer.writeKeyValue("ModifiedTime", modifiedTime);
        writer.writeKeyValue("IsFile", isFile);
        writer.endObject();
    }
}
