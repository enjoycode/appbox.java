import java.util.List;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.BiPredicate;

public class TestORM {
    public abstract class EntityBase {}

    public class Product extends EntityBase {
        public int    id;
        public String name;
    }

    public class OrderItem extends EntityBase {
        public int     productId;
        public Product product;
        public int     quantity;
        public int     unitPrice;
    }

    public class SqlQuery<T extends EntityBase> {
        public SqlQuery<T> where(Predicate<T> filter) {return this;}

        public SqlQuery<T> andWhere(Predicate<T> filter) {return this;}

        public <J extends EntityBase> void leftJoin(SqlQuery<J> right, BiPredicate<T, J> join) {}

        public List<T> toList() {return null;}

        public <R> List<R> toList(Function<? super T, ? extends R> mapper) {return null;}

        public <R, J extends EntityBase> List<R> toList(SqlQuery<J> j, BiFunction<? super T, ? super J, ? extends R> mapper) {
            return null;
        }
    }

    public void test() {
        var q = new SqlQuery<OrderItem>();
        q.where(t -> t.productId == 1 && t.unitPrice >= 100);

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

}
