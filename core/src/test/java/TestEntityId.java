import appbox.data.EntityId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class TestEntityId {

    @Test
    public void testEntityId() {
        var eid = new EntityId();
        eid.initRaftGroupId(0x010203040506L);
        assertEquals(0x010203040506L, eid.raftGroupId());
        //eid.initRaftGroupId(-1L); 不可能负数
        //assertEquals(-1L , eid.raftGroupId());

        LocalDateTime ts = Instant.ofEpochMilli(eid.timestamp()).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
        System.out.println(ts);
    }

}
