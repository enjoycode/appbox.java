import com.github.jasync.sql.db.*;
import com.github.jasync.sql.db.general.ArrayRowData;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TestJasyncPG {

    @Test
    public void testCreateTable() throws ExecutionException, InterruptedException {
        Configuration configuration=new Configuration("test","127.0.0.1",5432,"123456","testdb");
        PostgreSQLConnectionFactory             factory    =new PostgreSQLConnectionFactory(configuration);
        PostgreSQLConnection connection =factory.create().get();
        // Connection to PostgreSQL DB
        //Connection connection = PostgreSQLConnectionBuilder.createConnectionPool(
        //        "jdbc:postgresql://127.0.0.1:5432/testdb?user=test&password=123456");
        CompletableFuture<QueryResult> future = connection.sendQuery("create table person(id int2,name varchar(30),age int2,PRIMARY KEY(id))");
        connection.disconnect().get();
        //QueryResult queryResult = future.get();
        //System.out.println(Arrays.toString(((ArrayRowData) (queryResult.getRows().get(0))).getColumns()));

    }

    @Test
    public void testInsert() throws ExecutionException, InterruptedException {
        Configuration configuration=new Configuration("test","127.0.0.1",5432,"123456","testdb");
        PostgreSQLConnectionFactory             factory    =new PostgreSQLConnectionFactory(configuration);
        PostgreSQLConnection connection =factory.create().get();
        // Connection to PostgreSQL DB
        //Connection connection = PostgreSQLConnectionBuilder.createConnectionPool(
        //        "jdbc:postgresql://127.0.0.1:5432/testdb?user=test&password=123456");

        CompletableFuture<QueryResult> future = connection.sendQuery("insert into person(id,name,age) values(4, 'Chen long', 54)");
        QueryResult result=future.get();
        //QueryResult queryResult = future.get();
        connection.disconnect().get();
        System.out.println(result.getRowsAffected());
    }

    @Test
    public void testSelect() throws ExecutionException, InterruptedException {
        // Connection to PostgreSQL DB
        Connection connection = PostgreSQLConnectionBuilder.createConnectionPool(
                "jdbc:postgresql://127.0.0.1:5432/testdb?user=test&password=123456");

        // Execute query
        CompletableFuture<QueryResult> future = connection.sendPreparedStatement("select * from person");
        // work with result ...
        // Close the connection pool
        //connection.disconnect().get();
        QueryResult queryResult = future.get();
        List<RowData> rows=queryResult.getRows();
        for(RowData row:rows){
            ArrayRowData data= (ArrayRowData) row;
            LinkedHashMap map= (LinkedHashMap) data.getMapping();
            Object[] arr=data.getColumns();
            Iterator iterator=map.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String,Integer> next = (Map.Entry<String, Integer>) iterator.next();
                System.out.println("key:"+next.getKey()+" value:"+arr[next.getValue()]);
            }
        }

        //System.out.println(Arrays.toString(((ArrayRowData) (queryResult.getRows().get(0))).getColumns()));
        //System.out.println(Arrays.toString(((ArrayRowData) (queryResult.getRows().get(1))).getColumns()));
        //System.out.println(Arrays.toString(((ArrayRowData) (queryResult.getRows().get(2))).getColumns()));
        //System.out.println(Arrays.toString(((ArrayRowData) (queryResult.getRows().get(3))).getColumns()));
        connection.disconnect().get();
    }
}
