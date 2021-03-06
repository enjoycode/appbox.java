import appbox.data.SysEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.MockRuntimeContext;
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
        public void writeMember(short id, IEntityMemberWriter bs, byte flags) {
            switch (id) {
                case 1:
                    bs.writeMember(id, name, flags);
                    break;
                case 2:
                    bs.writeMember(id, age, flags);
                    break;
                default:
                    throw new UnknownEntityMember(Emploee.class, id);
            }
        }

        @Override
        public void readMember(short id, IEntityMemberReader bs, int flags) {
            switch (id) {
                case 1:
                    name = bs.readStringMember(flags);
                    break;
                case 2:
                    age = bs.readIntMember(flags);
                    break;
                default:
                    throw new UnknownEntityMember(Emploee.class, id);
            }
        }
    }

    @Test
    public void test1() {
        var ctx = new MockRuntimeContext();
        RuntimeContext.init(ctx, (short) 10421);

        var appModel = new ApplicationModel("appbox", "sys");
        ctx.injectApplicationModel(appModel);
        var empModel   = new EntityModel(12345678L, "Emploee");
        empModel.bindToSysStore(true, false);
        var nameMember = new DataFieldModel(empModel, "Name", DataFieldModel.DataFieldType.String, true);
        empModel.addSysMember(nameMember, (short) 1);
        var ageMember = new DataFieldModel(empModel, "Age", DataFieldModel.DataFieldType.Int, false);
        empModel.addSysMember(ageMember, (short) 2);
        ctx.injectEntityModel(empModel);


        var emp = new Emploee();
        emp.setName("Rick");
        emp.setAge(33);

        var outStream  = new BytesOutputStream(1000);
        emp.writeTo(outStream);
    }

}
