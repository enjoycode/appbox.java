package appbox.data;

import java.util.UUID;

/**
 * 用于描述树型Entity节点的全路径(目前仅适用于带UUID主键的树)
 */
public final class TreeNodePath {
    //region ====TreeNodeInfo====
    public static final class TreeNodeInfo {
        public final UUID id;
        public final String text;

        public TreeNodeInfo(UUID id, String text) {
            this.id = id;
            this.text = text;
        }
    }
    //endregion

    private final TreeNodeInfo[] nodes;

    public TreeNodePath(TreeNodeInfo[] nodes) {
        this.nodes = nodes;
    }

    public int level() { return nodes.length; }

    public TreeNodeInfo getAt(int level) {
        return nodes[level];
    }

}
