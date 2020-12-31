import appbox.entities.Employee;
import kotlin.collections.AbstractCollection;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.*;

public class TestORM {
    public abstract class EntityBase {}

    public abstract class IndexBase<T extends EntityBase> {}

    public class Product extends EntityBase {
        public int    id;
        public String name;
    }

    public class OrderItem extends EntityBase {
        public int     productId;
        public Product product;
        public int     quantity;
        public int     unitPrice;

        public class UI_ProductId extends IndexBase<OrderItem> {
            public int productId;

            public int getQuantity() {return 0;}
        }
    }

    public class KVIndexGet<E extends EntityBase, T extends IndexBase<E>> {
        public void where(Predicate<T> filter) {}

        public T toIndexRow() { return null; }

        public E toEntity() { return null; }
    }

    public static class DbFunc {
        public static <T> boolean test(T field1, T field2) { throw new RuntimeException(); }

        public static <T extends Number> T sum(T field) {return null;}

        public static <T> boolean in(T field, SqlSubQuery<T> subQuery) { return false; }

        //public static <T> boolean in(T field, Collection<T> subQuery) { return false; }
    }

    public class SqlQuery<T extends EntityBase> {
        public Object[] select(Object... item) {return null;}

        public SqlQuery<T> where(Predicate<T> filter) {return this;}

        public SqlQuery<T> andWhere(Predicate<T> filter) {return this;}

        public <J extends EntityBase> void leftJoin(SqlQuery<J> right, BiPredicate<T, J> join) {}

        public SqlQuery<T> groupBy(Function<? super T, Object[]> selector) {
            return this;
        }

        public List<T> toList() {return null;}

        public <R> List<R> toList(Function<? super T, ? extends R> mapper) {return null;}

        public <R, J extends EntityBase> List<R> toList(SqlQuery<J> j, BiFunction<? super T, ? super J, ? extends R> mapper) {
            return null;
        }

        public <R> SqlSubQuery<R> toSubQuery(Function<? super T, ? extends R> select) {
            return null;
        }

        //public <R> SqlSubQuery<? super R> toSubQuery(Function<? super T, ? extends R> select) {
        //    return null;
        //}
    }

    public class SqlSubQuery<T> extends ArrayList<T> {

    }

    public void testSqlQuery() {
        var q = new SqlQuery<OrderItem>();
        q.where(t -> t.productId == 1 && t.unitPrice >= 100 && t.product.name.contains("aa"));
        q.groupBy(t -> q.select(t.productId, t.unitPrice));

        var j = new SqlQuery<Product>();
        q.leftJoin(j, (orderItem, product) -> orderItem.productId == product.id);

        var joinList = q.toList(j, (orderItem, product) -> new Object() {
            String pname = product.name;
            int quantity = orderItem.quantity;
        });

        var expandList = q.toList(o -> new OrderItem() {
            String productName = o.product.name;
        });

        var list = q.toList(o -> new Object() {
            final int pid = o.productId;
            final String pname = o.product.name;
        });
        for (var item : list) {
            System.out.println(item.pname);
        }
    }

    public void testSubQuery() throws ClassNotFoundException {
        var sq = new SqlQuery<Product>();
        sq.where(product -> product.name == "aa");
        var             ssq = sq.toSubQuery(s -> s.name);

        var q = new SqlQuery<OrderItem>();
        q.where(o -> DbFunc.sum(o.productId) > 0);
        q.where(o -> DbFunc.test("aa", 1234));
        q.where(o -> DbFunc.test(o.productId, o.product.name));
        //q.where(o -> DbFunc.in(o.productId, ssq));
        q.where(o -> DbFunc.in(o.productId,
                sq.toSubQuery(s -> s.name)
        ));
    }

    public void testIndexGet() {
        var q = new KVIndexGet<OrderItem, OrderItem.UI_ProductId>();
        q.where(t -> t.productId == 1);
        var r1 = q.toIndexRow();
        var r2 = q.toEntity();
    }
}
