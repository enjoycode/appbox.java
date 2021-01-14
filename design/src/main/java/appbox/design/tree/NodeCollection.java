package appbox.design.tree;

import appbox.serialization.IJsonSerializable;
import appbox.serialization.IJsonWriter;

import java.util.*;
import java.util.function.Predicate;

/**
 * 子节点，添加时自动排序
 */
public final class NodeCollection implements IJsonSerializable {

    public final    DesignNode       owner; //Maybe null
    protected final List<DesignNode> list;

    public NodeCollection(DesignNode owner) {
        this.owner = owner;
        list       = new ArrayList<>();
    }

    public int add(DesignNode item) {
        item.setParent(owner);
        //特定owner找到插入点
        if (owner != null &&
                (owner.nodeType() == DesignNodeType.ModelRootNode
                        || owner.nodeType() == DesignNodeType.FolderNode)) {
            int index = -1;
            for (var i = 0; i < list.size(); i++) {
                if (item.compareTo(list.get(i)) < 0) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                list.add(index, item);
                return index;
            }

            list.add(item);
            return list.size() - 1;
        }

        list.add(item);
        return list.size() - 1;
    }

    public void remove(DesignNode item) {
        int index = list.indexOf(item);
        if (index >= 0) {
            item.setParent(null);
            list.remove(index);
        }
    }

    public void clear() {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setParent(null);
        }
        list.clear();
    }

    public DesignNode find(Predicate<DesignNode> match) {
        for (DesignNode node : list) {
            if (match.test(node)) {
                return node;
            }
        }
        return null;
    }

    public boolean exists(Predicate<DesignNode> match) {
        for (DesignNode node : list) {
            if (match.test(node)) {
                return true;
            }
        }
        return false;
    }

    public int count() {
        return list.size();
    }

    public DesignNode get(int index) {
        return list.get(index);
    }

    //region ====IJsonSerializable====
    @Override
    public void writeToJson(IJsonWriter writer) {
        writer.startArray();
        for (DesignNode node : list) {
            node.writeToJson(writer);
        }
        writer.endArray();
    }
    //endregion

}
