package appbox.design.event;

import appbox.channel.IClientMessage;
import appbox.design.IDeveloperSession;
import appbox.serialization.IOutputStream;
import org.dartlang.analysis.server.protocol.FoldingRegion;

import java.util.List;

public final class CodeFoldingEvent implements IClientMessage {
    private final long                modelId;
    private final List<FoldingRegion> regions;

    public CodeFoldingEvent(long modelId, List<FoldingRegion> regions) {
        this.modelId = modelId;
        this.regions = regions;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(IDeveloperSession.CODE_EVENT);
        bs.writeByte(CodeEventType.FOLDING);

        bs.writeLong(modelId);
        bs.writeVariant(regions.size());
        for (var region : regions) {
            bs.writeByte(mapRegionKind(region.getKind()));
            bs.writeVariant(region.getOffset());
            bs.writeVariant(region.getLength());
        }
    }

    private static byte mapRegionKind(String kind) {
        switch (kind) {
            case "ANNOTATIONS":
                return 1;
            case "BLOCK":
                return 2;
            case "CLASS_BODY":
                return 3;
            case "COMMENT":
                return 4;
            case "DIRECTIVES":
                return 5;
            case "DOCUMENTATION_COMMENT":
                return 6;
            case "FILE_HEADER":
                return 7;
            case "FUNCTION_BODY":
                return 8;
            case "INVOCATION":
                return 9;
            case "LITERAL":
                return 10;
            case "PARAMETERS":
                return 11;
            default:
                return 0;
        }
    }

}
