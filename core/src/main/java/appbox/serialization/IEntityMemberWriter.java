package appbox.serialization;

import java.util.Optional;

public interface IEntityMemberWriter {

    void writeMember(short id, String value, byte storeFlags) throws Exception;

    void writeMember(short id, int value, byte storeFlags) throws Exception;

    void writeMember(short id, Optional<Integer> value, byte storeFlags) throws Exception;

}
