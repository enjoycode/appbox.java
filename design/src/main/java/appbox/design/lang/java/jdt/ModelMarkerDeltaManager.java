package appbox.design.lang.java.jdt;

import org.eclipse.core.internal.resources.MarkerDelta;
import org.eclipse.core.internal.resources.MarkerSet;
import org.eclipse.core.runtime.IPath;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ModelMarkerDeltaManager {
    private static final int        DEFAULT_SIZE = 10;
    private long[]                  startIds = new long[10];
    private Map<IPath, MarkerSet>[] batches  = new Map[10];
    private int                     nextFree = 0;

    ModelMarkerDeltaManager() {
    }

    protected Map<IPath, MarkerSet> assembleDeltas(long start) {
        Map<IPath, MarkerSet> result = null;

        for(int i = 0; i < this.nextFree; ++i) {
            if (this.startIds[i] >= start) {
                result = MarkerDelta.merge(result, this.batches[i]);
            }
        }

        return result;
    }

    protected void resetDeltas(long startId) {
        int startOffset;
        for(startOffset = 0; startOffset < this.nextFree && this.startIds[startOffset] < startId; ++startOffset) {
        }

        if (startOffset != 0) {
            long[] newIds = this.startIds;
            Map[] newBatches = this.batches;
            if (this.startIds.length > 10 && this.nextFree - startOffset < 10) {
                newIds = new long[10];
                newBatches = new Map[10];
            }

            int remaining = this.nextFree - startOffset;
            System.arraycopy(this.startIds, startOffset, newIds, 0, remaining);
            System.arraycopy(this.batches, startOffset, newBatches, 0, remaining);
            Arrays.fill(this.startIds, remaining, this.startIds.length, 0L);
            Arrays.fill(this.batches, remaining, this.startIds.length, (Object)null);
            this.startIds = newIds;
            this.batches = newBatches;
            this.nextFree = remaining;
        }
    }

    protected Map<IPath, MarkerSet> newGeneration(long start) {
        int len = this.startIds.length;
        if (this.nextFree >= len) {
            long[] newIds = new long[len * 2];
            Map[] newBatches = new Map[len * 2];
            System.arraycopy(this.startIds, 0, newIds, 0, len);
            System.arraycopy(this.batches, 0, newBatches, 0, len);
            this.startIds = newIds;
            this.batches = newBatches;
        }

        this.startIds[this.nextFree] = start;
        this.batches[this.nextFree] = new HashMap(11);
        return this.batches[this.nextFree++];
    }
}
