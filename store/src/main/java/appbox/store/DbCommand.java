package appbox.store;

import appbox.data.EntityId;
import appbox.logging.Log;
import appbox.serialization.IEntityMemberWriter;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** 仅用于包装Sql命令及相应的参数 */
final class DbCommand implements IEntityMemberWriter {

    private String       _commandText;
    private List<Object> _parameters;

    protected Connection connection; //仅用于暂存临时连接

    public String getCommandText() { return _commandText; }

    public void setCommandText(String cmd) { _commandText = cmd; }

    protected List<Object> getParameters() { return _parameters; }

    public void addParameter(Object parameter) {
        if (_parameters == null)
            _parameters = new ArrayList<>();
        _parameters.add(parameter);
    }

    //region ====Exec Methods====
    protected CompletableFuture<Long> execNonQueryAsync(Connection conn) {
        return execQueryAsync(conn).thenApply(QueryResult::getRowsAffected);
    }

    protected CompletableFuture<QueryResult> execQueryAsync(Connection conn) {
        if (conn == null)
            throw new IllegalArgumentException("Can't exec command without connection");
        this.connection = conn;

        Log.debug(String.format("Exec sql: %s", _commandText));
        if (_parameters != null && _parameters.size() > 0) {
            return this.connection.sendPreparedStatement(_commandText, _parameters, true);
        } else {
            return this.connection.sendQuery(_commandText);
        }
    }
    //endregion

    //region ====IEntityMemberWriter====
    //注意构建更新命令时需要写入null成员

    @Override
    public void writeMember(short id, EntityId value, byte flags) throws Exception {
        if (value != null)
            addParameter(value.toString()); //暂转换为字符串，待检查
        else
            addParameter(null);
    }

    @Override
    public void writeMember(short id, String value, byte flags) throws Exception {
        if (value != null) {
            addParameter(value);
        } else if ((flags & IEntityMemberWriter.SF_WRITE_NULL) == IEntityMemberWriter.SF_WRITE_NULL) {
            addParameter(null);
        }
    }

    @Override
    public void writeMember(short id, int value, byte flags) throws Exception {
        addParameter(value);
    }

    @Override
    public void writeMember(short id, Optional<Integer> value, byte flags) throws Exception {
        if (value.isPresent()) {
            addParameter(value.get());
        } else if ((flags & IEntityMemberWriter.SF_WRITE_NULL) == IEntityMemberWriter.SF_WRITE_NULL) {
            addParameter(null);
        }
    }

    @Override
    public void writeMember(short id, UUID value, byte flags) throws Exception {
        if (value != null)
            addParameter(value.toString()); //暂转换为字符串，待检查
        else
            addParameter(null);
    }

    @Override
    public void writeMember(short id, byte[] value, byte flags) throws Exception {
        addParameter(value);
    }

    @Override
    public void writeMember(short id, boolean value, byte flags) throws Exception {
        addParameter(value);
    }

    @Override
    public void writeMember(short id, Date value, byte flags) throws Exception {
        if (value != null)
            addParameter(String.valueOf(value.getTime())); //暂转换为字符串，待检查
        else
            addParameter(null);
    }

    //endregion

}
