package appbox.store.query;

import appbox.data.EntityId;
import appbox.serialization.IEntityMemberReader;
import com.github.jasync.sql.db.RowData;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 用于包装RowData */
public final class SqlRowReader implements IEntityMemberReader {
    public final List<String> columns;
    public       RowData      rowData;

    public SqlRowReader(List<String> columns) {
        this.columns = columns;
    }

    public boolean isNull(int col) {
        return rowData.get(col) == null;
    }

    //====GetXXX Methods, 用于填充动态类型的成员====
    public int getInt(int col) {
        return rowData.getInt(col);
    }

    public String getString(int col) {
        return rowData.getString(col);
    }

    //====以下用于填充实体，其中flags是列序号====

    @Override
    public String readStringMember(int flags) {
        return rowData.getString(flags);
    }

    @Override
    public boolean readBoolMember(int flags) {
        return rowData.getBoolean(flags);
    }

    @Override
    public int readIntMember(int flags) {
        return rowData.getInt(flags);
    }

    @Override
    public byte readByteMember(int flags) {
        return rowData.getByte(flags);
    }

    @Override
    public UUID readUUIDMember(int flags) {
        var uuidString = rowData.getString(flags); //暂从字符串转回，TODO:待检查
        return UUID.fromString(uuidString);
    }

    @Override
    public byte[] readBinaryMember(int flags) {
        return (byte[]) rowData.get(flags);
    }

    @Override
    public Date readDateMember(int flags) {
        return new Date(rowData.getLong(flags));
    }

    @Override
    public EntityId readEntityIdMember(int flags) {
        throw new UnsupportedOperationException();
    }
}
