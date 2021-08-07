package appbox.data;

import appbox.logging.Log;
import appbox.serialization.IInputStream;
import appbox.serialization.IOutputStream;

import java.util.ArrayList;
import java.util.List;

/** 映射至存储的实体基类 */
public abstract class DBEntity extends Entity {

    private PersistentState _persistentState = PersistentState.Detached;
    private List<Short>     _changedMembers  = null;

    /** 实体持久化状态 */
    public final PersistentState persistentState() {return _persistentState;}

    protected final void onPropertyChanged(short memberId) {
        if (_persistentState == PersistentState.Unchnaged) {
            _persistentState = PersistentState.Modified;

            //track member changes
            if (_changedMembers == null) {
                _changedMembers = new ArrayList<>();
                _changedMembers.add(memberId);
            } else {
                if (_changedMembers.stream().noneMatch(t -> t == memberId)) {
                    _changedMembers.add(memberId);
                }
            }
        }
    }

    //TODO: acceptChanges()

    /** 标记实体为删除状态 */
    public final void markDeleted() {
        //新建的暂直接报错
        if (_persistentState == PersistentState.Detached)
            throw new RuntimeException("Detached entity can't mark to deleted");

        _persistentState = PersistentState.Deleted;
    }

    /** 从数据库加载完后变更持久化状态 */
    public final void fetchDone() {
        _persistentState = PersistentState.Unchnaged;
    }

    /**
     * 从数据库加载时根据名称获取导航属性实例
     * @return EntityRef成员返回DbEntity实例，EntitySet成员返回List<DbEntity>
     */
    public Object getNaviPropForFetch(String propName) { //TODO:考虑参数baseType:long用于聚合引用
        //TODO:考虑从ServiceClassLoader内实例化,或者从EntityFactoryMap内实例化
        throw new UnsupportedOperationException(propName);
    }

    @Override
    public void writeTo(IOutputStream bs) {
        super.writeTo(bs);

        bs.writeByte(_persistentState.value);
        bs.writeListShort(_changedMembers);

        //写入匿名类扩展信息,仅用于向前端输出
        if (this.getClass().isAnonymousClass()) {
            var fields = this.getClass().getFields();
            bs.writeVariant(fields.length);
            try {
                for (var field : fields) {
                    field.setAccessible(true);
                    bs.writeString(field.getName());
                    bs.serialize(field.get(this));
                }
            } catch (Exception ex) {
                Log.error("序列化实体扩展信息错误: " + ex);
                throw new RuntimeException(ex);
            }
        } else {
            bs.writeVariant(0);
        }
    }

    @Override
    public void readFrom(IInputStream bs) {
        super.readFrom(bs);

        _persistentState = PersistentState.fromValue(bs.readByte());
        _changedMembers  = bs.readListShort();

        //读取扩展字段信息，暂忽略
        final var extFields = bs.readVariant();
        if (extFields > 0) {
            for (int i = 0; i < extFields; i++) {
                final var fieldName  = bs.readString();
                final var fieldValue = bs.deserialize();
                Log.debug("Read ext field: " + fieldName + "=" + fieldValue);
            }
        }
    }
}
