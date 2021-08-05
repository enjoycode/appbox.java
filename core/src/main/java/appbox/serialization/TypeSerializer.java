package appbox.serialization;

import appbox.data.JsonResult;
import appbox.expressions.BinaryExpression;
import appbox.expressions.KVFieldExpression;
import appbox.expressions.PrimitiveExpression;
import appbox.logging.Log;
import appbox.model.*;
import appbox.serialization.serializers.*;

import java.util.HashMap;
import java.util.function.Supplier;

public abstract class TypeSerializer {

    private static final HashMap<Class<?>, TypeSerializer> _knownTypes    = new HashMap<>();
    private static final HashMap<Byte, TypeSerializer>     _sysKnownTypes = new HashMap<>();

    static {
        //实例创建返回null表示不支持反序列化
        Log.debug("Register type serializer...");
        registerKnownType(IntSerializer.instance);
        registerKnownType(ByteSerializer.instance);
        registerKnownType(LongSerializer.instance);
        registerKnownType(StringSerializer.instance);
        registerKnownType(UUIDSerializer.instance);
        registerKnownType(EntityIdSerializer.instance);

        registerKnownType(new UserSerializer(PayloadType.JsonObject, JsonResult.class, () -> null));

        //----模型相关----
        registerKnownType(new UserSerializer(PayloadType.DataStoreModel, DataStoreModel.class, DataStoreModel::new));
        registerKnownType(new UserSerializer(PayloadType.ViewModel, ViewModel.class, ViewModel::new));
        registerKnownType(new UserSerializer(PayloadType.EntityModel, EntityModel.class, EntityModel::new));
        registerKnownType(new UserSerializer(PayloadType.ServiceModel, ServiceModel.class, ServiceModel::new));
        registerKnownType(new UserSerializer(PayloadType.PermissionModel, PermissionModel.class, PermissionModel::new));
        registerKnownType(new UserSerializer(PayloadType.ModelFolder, ModelFolder.class, ModelFolder::new));
        registerKnownType(new UserSerializer(PayloadType.EntityModelInfo, EntityModelInfo.class, () -> null));

        //----表达式相关(目前都不支持反序列化)----
        registerKnownType(new UserSerializer(PayloadType.BinaryExpression, BinaryExpression.class, () -> null));
        registerKnownType(new UserSerializer(PayloadType.PrimitiveExpression, PrimitiveExpression.class, () -> null));
        registerKnownType(new UserSerializer(PayloadType.KVFieldExpression, KVFieldExpression.class, () -> null));
    }

    /** 注册已知类型的序列化器 */
    public static void registerKnownType(TypeSerializer serializer) {
        _knownTypes.put(serializer.targetType, serializer);
        if (serializer.payloadType == PayloadType.ExtKnownType) {
            //TODO:
        }
        _sysKnownTypes.put(serializer.payloadType, serializer);
    }

    /** 序列化时根据目标类型获取相应的序列化实现 */
    public static TypeSerializer getSerializer(Class<?> type) {
        //TODO: 范型及容器类型的处理
        //TypeSerializer serializer = null;
        //var targetType = type;
        //if (type.getGenericInterfaces())

        return _knownTypes.get(type);
    }

    /** 反序列化时根据PayloadType获取相应的系统已知类型的序列化实现 */
    public static TypeSerializer getSerializer(byte payloadType) {
        return _sysKnownTypes.get(payloadType);
    }

    public final byte payloadType;

    public final Class<?> targetType;

    public final Supplier<Object> creator;

    protected TypeSerializer(byte payloadType, Class<?> targetType, Supplier<Object> creator) {
        this.payloadType = payloadType;
        this.targetType  = targetType;
        this.creator     = creator;
    }

    public abstract void write(IOutputStream bs, Object value);

    public abstract Object read(IInputStream bs, Object value);
}
