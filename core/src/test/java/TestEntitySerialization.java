import appbox.data.SysEntity;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.RuntimeContext;
import appbox.serialization.*;
import org.junit.jupiter.api.Test;

public class TestEntitySerialization {
    //测试实体类
    public static final class Emploee extends SysEntity {

        private String name;
        private int    age;

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int value) {
            age = value;
        }

        public Emploee() {
            super(12345678L);
        }

        @Override
        public void writeMember(short id, IEntityMemberWriter bs, byte storeFlags) throws Exception {
            switch (id) {
                case 1:
                    bs.writeMember(id, name, storeFlags);
                    break;
                case 2:
                    bs.writeMember(id, age, storeFlags);
                    break;
                default:
                    throw new Exception("unknown member");
            }
        }

        @Override
        public void readMember(short id, IEntityMemberReader bs, int storeFlags) throws Exception {
            switch (id) {
                case 1:
                    name = bs.readStringMember(storeFlags);
                    break;
                case 2:
                    age = bs.readIntMember(storeFlags);
                    break;
                default:
                    throw new Exception("unknown member");
            }
        }
    }

    @Test
    public void test1() throws Exception {
        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var appModel = new ApplicationModel("appbox", "sys");
        ctx.injectApplicationModel(appModel);
        var empModel   = new EntityModel(12345678L, "Emploee", true, false);
        var nameMember = new DataFieldModel(empModel, "Name", DataFieldModel.DataFieldType.String, true, false);
        empModel.addSysMember(nameMember, (short) 1);
        var ageMember = new DataFieldModel(empModel, "Age", DataFieldModel.DataFieldType.Int, false, false);
        empModel.addSysMember(ageMember, (short) 2);
        ctx.injectEntityModel(empModel);


        var emp = new Emploee();
        emp.setName("Rick");
        emp.setAge(33);

        var outStream  = new BytesOutputStream(1000);
        var serializer = BinSerializer.rentFromPool(outStream);
        emp.writeTo(serializer);

        BinSerializer.backToPool(serializer);
    }

}
