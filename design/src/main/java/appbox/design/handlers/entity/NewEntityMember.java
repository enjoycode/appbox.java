package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.EntitySetModel;
import appbox.runtime.InvokeArgs;

import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

public final class NewEntityMember implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId          = args.getString();
        var memberName       = args.getString();
        var entityMemberType = args.getInt();

        var node = hub.designTree.findModelNode(ModelType.Entity, Long.parseUnsignedLong(modelId));
        if (node == null)
            throw new RuntimeException("Can't find entity model node");
        var model = (EntityModel) node.model();
        if (!node.isCheckoutByMe())
            throw new RuntimeException("Node has not checkout");
        if (!CodeHelper.isValidIdentifier(memberName))
            throw new RuntimeException("Name is invalid");
        if (memberName.equals(model.name()))
            throw new RuntimeException("Name can't same with Entity's name");
        if (model.tryGetMember(memberName) != null)
            throw new RuntimeException("Name has exists");

        EntityMemberModel memberModel = null;
        if (entityMemberType == EntityMemberModel.EntityMemberType.DataField.value) {
            memberModel = newDataField(model, memberName, args);
        } else if (entityMemberType == EntityMemberModel.EntityMemberType.EntityRef.value) {
            memberModel = newEntityRef(hub, model, memberName, args);
        } else if (entityMemberType == EntityMemberModel.EntityMemberType.EntitySet.value) {
            memberModel = newEntitySet(hub, model, memberName, args);
        } else {
            throw new RuntimeException("未实现");
        }

        //保存至Staged
        final var res = new JsonResult(memberModel);
        return node.saveAsync(null).thenApply(r -> {
            //更新虚拟代码
            hub.typeSystem.updateModelDocument(node);
            return res;
        });
    }

    private static EntityMemberModel newDataField(EntityModel model, String name, InvokeArgs args) {
        var fieldType = args.getInt();
        var allowNull = args.getBool();

        var df = new DataFieldModel(model, name, DataFieldModel.DataFieldType.fromValue((byte) fieldType), allowNull);
        model.addMember(df);
        if (!allowNull) { //注意：必须在model.AddMember之后，否则mid为0
            var defaultValueString = args.getString();
            if (defaultValueString != null && !defaultValueString.isEmpty()) {
                try {
                    df.setDefaultValue(defaultValueString);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return df;
    }

    private static EntityMemberModel newEntityRef(DesignHub hub, EntityModel model, String name, InvokeArgs args) {
        //TODO:***非映射至存储的不需要生成外键成员

        var allowNull = args.getBool();
        var refIdStr  = args.getString();
        var isReverse = args.getBool(); //是否反向引用
        if (refIdStr == null || refIdStr.isEmpty())
            throw new RuntimeException("EntityRef's target is null");

        //解析并检查所有引用类型的正确性
        var refIds    = refIdStr.split(",");
        var refModels = new EntityModel[refIds.length];
        for (int i = 0; i < refIds.length; i++) {
            var refNode = hub.designTree.findNode(DesignNodeType.EntityModelNode, refIds[i]);
            if (!(refNode instanceof ModelNode))
                throw new RuntimeException("EntityRef target not exists");
            var refModel = (EntityModel) ((ModelNode) refNode).model();
            if (model.storeOptions() != null && refModel.storeOptions() != null) {
                if (model.storeOptions().getClass() != refModel.storeOptions().getClass())
                    throw new RuntimeException("Can't reference to different store");
                if (model.sqlStoreOptions() != null
                        && model.sqlStoreOptions().storeModelId() != refModel.sqlStoreOptions().storeModelId())
                    throw new RuntimeException("Can't reference to different store");
                if (model.sqlStoreOptions() != null && !refModel.sqlStoreOptions().hasPrimaryKeys())
                    throw new RuntimeException("Can't reference to entity without primary key");
            } else {
                throw new RuntimeException("Can't reference to diffrent store");
            }
            refModels[i] = refModel;

            //检查所有主键的数量、类型是否一致
            //if (model.sqlStoreOptions() != null && i > 0) {
            //TODO:
            //}
        }

        //检查外键字段名称是否已存在，并且添加外键成员
        //if (refIds.Length > 1 && model.Members.FindIndex(t => t.Name == $"{name}Type") >= 0)
        //    throw new Exception($"Name has exists: {name}Type");
        var fkMemberIds = new short[refModels.length];
        if (model.sqlStoreOptions() != null) {
            //聚合引用以第一个的主键作为外键的名称
            var refModel = refModels[0];
            for (int i = 0; i < refModel.sqlStoreOptions().primaryKeys().length; i++) {
                var pk            = refModel.sqlStoreOptions().primaryKeys()[0];
                var pkMemberModel = (DataFieldModel) refModel.getMember(pk.memberId);
                var fkName        = String.format("%s%s", name, pkMemberModel.name());
                if (model.tryGetMember(fkName) != null)
                    throw new RuntimeException("Name has exists: " + fkName);
                var fk = new DataFieldModel(model, fkName, pkMemberModel.dataType(), allowNull, true);
                model.addMember(fk);
                fkMemberIds[i] = fk.memberId();
            }
        } else {
            var fkName = name + "Id";
            if (model.tryGetMember(fkName) != null)
                throw new RuntimeException("Name has exists: " + fkName);
            // 添加外键Id列, eg: Customer -> CustomerId
            var fkId = new DataFieldModel(model, fkName, DataFieldModel.DataFieldType.EntityId, allowNull, true);
            model.addMember(fkId);
            fkMemberIds[0] = fkId.memberId();
        }

        //新建EntityRefModel成员
        EntityRefModel erf = null;
        if (refIds.length > 1) {
            //如果为聚合引用则添加对应的Type列, eg: CostBill -> CostBillType
            throw new RuntimeException("未实现");
        } else {
            //TODO:入参指明是否外键约束
            erf = new EntityRefModel(model, name, Long.parseUnsignedLong(refIds[0]), fkMemberIds, true);

        }
        erf.setAllowNull(allowNull);
        model.addMember(erf);
        return erf;
    }

    private static EntityMemberModel newEntitySet(DesignHub hub, EntityModel model, String name, InvokeArgs args) {
        var refModelId  = Long.parseUnsignedLong(args.getString());
        var refMemberId = (short) args.getInt();
        //验证引用目标是否存在
        var target = hub.designTree.findModelNode(ModelType.Entity, refModelId);
        if (target == null)
            throw new RuntimeException("Can't find EntityRef");
        var targetModel  = (EntityModel) target.model();
        var targetMember = targetModel.getMember(refMemberId);
        if (targetMember.type() != EntityMemberModel.EntityMemberType.EntityRef)
            throw new RuntimeException("RefMember is not EntityRef");

        var esm = new EntitySetModel(model, name, refModelId, refMemberId);
        model.addMember(esm);
        return esm;
    }
}
