package appbox.store.query;

import appbox.serialization.IEntityMemberReader;
import com.github.jasync.sql.db.RowData;

import java.util.List;

/** 用于包装RowData */
final class SqlRowReader implements IEntityMemberReader {
    public final List<String> columns;
    public       RowData      rowData;

    public SqlRowReader(List<String> columns) {
        this.columns = columns;
    }

    public boolean isNull(int col) {
        return rowData.get(col) == null;
    }

    //以下flags是列序号

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
}
