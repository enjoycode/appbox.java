import appbox.data.SysEntity;
import appbox.exceptions.UnknownEntityMember;
import appbox.model.ApplicationModel;
import appbox.model.EntityModel;
import appbox.model.entity.DataFieldModel;
import appbox.runtime.MockRuntimeContext;
import appbox.runtime.RuntimeContext;
import appbox.serialization.*;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class TestEntitySerialization {
    //测试实体类
    public static final class Emploee extends SysEntity {

        public static final long MODELID = 12345678L;

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

        @Override
        public long modelId() {
            return MODELID;
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
        var empModel = new EntityModel(12345678L, "Emploee");
        empModel.bindToSysStore(true, false);
        var nameMember = new DataFieldModel(empModel, "Name", DataFieldModel.DataFieldType.String, true);
        empModel.addSysMember(nameMember, (short) 1);
        var ageMember = new DataFieldModel(empModel, "Age", DataFieldModel.DataFieldType.Int, false);
        empModel.addSysMember(ageMember, (short) 2);
        ctx.injectEntityModel(empModel);

        var emp = new Emploee();
        emp.setName("Rick");
        emp.setAge(33);

        var outStream = new BytesOutputStream(1000);
        outStream.serialize(emp);

        var input = new BytesInputStream(outStream.toByteArray());
        var emp2  = input.deserializeEntity(Emploee::new);
        assertFalse(input.hasRemaining());
        assertEquals("Rick", emp.getName());
        assertEquals(33, emp.getAge());
    }

    public static class OrgUnit extends SysEntity {
        //static public java.util.concurrent.CompletableFuture<OrgUnit> fetchAsync(  appbox.data.EntityId id){
        //    return appbox.store.EntityStore.loadAsync(OrgUnit.class,id);
        //}
        public static final long   MODELID = -7018111290459553776L;
        private             String _Name;

        public String getName() {
            return _Name;
        }

        public void setName(String value) {
            _Name = value;
            onPropertyChanged((short) 128);
        }

        private appbox.data.EntityId _BaseId;

        public appbox.data.EntityId getBaseId() {
            return _BaseId;
        }

        public void setBaseId(appbox.data.EntityId value) {
            _Base   = null;
            _BaseId = value;
            onPropertyChanged((short) 256);
        }

        private long _BaseType;

        public long getBaseType() {
            return _BaseType;
        }

        public void setBaseType(long value) {
            _Base     = null;
            _BaseType = value;
            onPropertyChanged((short) 384);
        }

        private appbox.data.SysEntity _Base;

        public appbox.data.SysEntity getBase() {
            return _Base;
        }

        public void setBase(appbox.data.SysEntity value) {
            setBaseId(value == null ? null : value.id());
            setBaseType(value == null ? null : value.modelId());
            _Base = value;
        }

        private appbox.data.EntityId _ParentId;

        public appbox.data.EntityId getParentId() {
            return _ParentId;
        }

        public void setParentId(appbox.data.EntityId value) {
            _Parent   = null;
            _ParentId = value;
            onPropertyChanged((short) 640);
        }

        private OrgUnit _Parent;

        public OrgUnit getParent() {
            return _Parent;
        }

        public void setParent(OrgUnit value) {
            setParentId(value == null ? null : value.id());
            _Parent = value;
        }

        private java.util.List<OrgUnit> _Childs;

        public java.util.List<OrgUnit> getChilds() {
            if (persistentState() == appbox.data.PersistentState.Detached && _Childs == null)
                _Childs = new java.util.ArrayList<OrgUnit>();
            return _Childs;
        }

        @Override
        public long modelId() {
            return MODELID;
        }

        @Override
        public void writeMember(short id, appbox.serialization.IEntityMemberWriter bs, byte flags) {
            switch (id) {
                case (short) 128:
                    bs.writeMember(id, _Name, flags);
                    break;
                case (short) 256:
                    bs.writeMember(id, _BaseId, flags);
                    break;
                case (short) 384:
                    bs.writeMember(id, _BaseType, flags);
                    break;
                case (short) 512:
                    bs.writeMember(id, _Base, flags);
                    break;
                case (short) 640:
                    bs.writeMember(id, _ParentId, flags);
                    break;
                case (short) 768:
                    bs.writeMember(id, _Parent, flags);
                    break;
                case (short) 896:
                    bs.writeMember(id, _Childs, flags);
                    break;
                default:
                    throw new appbox.exceptions.UnknownEntityMember(OrgUnit.class, id);
            }
        }

        @Override
        public void readMember(short id, appbox.serialization.IEntityMemberReader bs, int flags) {
            switch (id) {
                case (short) 128:
                    _Name = bs.readStringMember(flags);
                    break;
                case (short) 256:
                    _BaseId = bs.readEntityIdMember(flags);
                    break;
                case (short) 384:
                    _BaseType = bs.readLongMember(flags);
                    break;
                case (short) 512:
                case (short) 640:
                    _ParentId = bs.readEntityIdMember(flags);
                    break;
                case (short) 768:
                    _Parent = bs.readRefMember(flags, OrgUnit::new);
                    break;
                case (short) 896:
                    _Childs = bs.readSetMember(flags, OrgUnit::new);
                    break;
                default:
                    throw new appbox.exceptions.UnknownEntityMember(OrgUnit.class, id);
            }
        }

        @Override
        public Object getNaviPropForFetch(String propName) {
            switch (propName) {
                case "Base":
                case "Parent":
                    if (_Parent == null) _Parent = new OrgUnit();
                    return _Parent;
                case "Childs":
                    if (_Childs == null) _Childs = new java.util.ArrayList<OrgUnit>();
                    return _Childs;
                default:
                    throw new java.lang.RuntimeException("OrgUnit");
            }
        }
    }

    @Test
    public void test2() {
        //从OrgUnitService.loadTree()获取
        final var base64 = "CwcAAAAAFBgCWhAAAAL3qJqegAAYQXBwQm94RnV0dXJlAAEAACABEAB4AXKdAJoQQQABgAEIAAAC96ianoADAloQAAAC96ianoAADklUIERlcHQAAQAAIAEwAHgBcp0AwxBBAASAAQwAAAL3qJqegAIAACABQAB4AXKdAMMQQQAFAAMWAIADBFoQAAAC96ianoAACkFkbWluAAEAACABIAB4AXKdAJsQQQACgAEEAAAC96ianoACAAAgAUAAeAFynQDDEEEABgADFgIAAAEBAAAAIAFAAHgBcp0AwxBBAAdaEAAAAveomp6AAAhUZXN0AAEAACABIAB4AXKdALsQQQADgAEEAAAC96ianoACAAAgAUAAeAFynQDDEEEABgADFgIAAAEBAAAAIAFAAHgBcp0AwxBBAAgAAAEBAAAAIAFAAHgBcp0AwxBBAAYAAAEBAAAAIAFAAHgBcp0AwxBBAAU=";
        final var data   = Base64.getDecoder().decode(base64);
        final var input  = new BytesInputStream(data);
        input.skip(6);

        var payloadType = input.readByte();
        assertEquals(PayloadType.List, payloadType);
        payloadType = input.readByte();
        assertEquals(PayloadType.Object, payloadType);

        var listSize = input.readVariant();
        assertEquals(1, listSize);

        var entity = input.deserializeEntity(OrgUnit::new);
        assertFalse(input.hasRemaining());

    }

}
