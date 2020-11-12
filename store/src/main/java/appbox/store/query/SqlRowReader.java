package appbox.store.query;

import appbox.serialization.IEntityMemberReader;
import com.github.jasync.sql.db.RowData;

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
    public String readStringMember(int flags) throws Exception {
        return rowData.getString(flags);
    }

    @Override
    public boolean readBoolMemeber(int flags) throws Exception {
        return rowData.getBoolean(flags);
    }

    @Override
    public int readIntMember(int flags) throws Exception {
        return rowData.getInt(flags);
    }

    @Override
    public byte readByteMember(int flags) throws Exception {
        return rowData.getByte(flags);
    }

    @Override
    public UUID readUUIDMember(int flags) throws Exception {
        return new UUID(rowData.getLong(flags), rowData.getLong(flags));
    }

    @Override
    public byte[] readBinaryMember(int flags) throws Exception {
        String s=rowData.getString(flags);
        return s.getBytes();
    }
}
