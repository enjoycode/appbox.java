package appbox.design.common;

import appbox.design.tree.DesignNodeType;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用于包装设计器向服务端发送的签出请求
 */
public final class CheckoutInfo {
    public final DesignNodeType nodeType;
    public final String         targetID;
    public final int            version;
    public final String         developerName;
    public final UUID           developerOuid;
    public final LocalDateTime  checkoutTime;

    public CheckoutInfo(DesignNodeType nodeType, String targetID,
                        int version, String developerName, UUID developerOuID) {
        this(nodeType, targetID, version, developerName, developerOuID, LocalDateTime.now());
    }

    public CheckoutInfo(DesignNodeType nodeType, String targetID,
                        int version, String developerName, UUID developerOuID, LocalDateTime checkoutTime) {
        this.nodeType = nodeType;
        this.targetID = targetID;
        this.version = version;
        this.developerName = developerName;
        this.developerOuid = developerOuID;
        this.checkoutTime = checkoutTime;
    }

    public boolean isSingleModel() {
        return nodeType.value >= DesignNodeType.EntityModelNode.value;
    }

    public String getKey() {
        return makeKey(nodeType, targetID);
    }

    public static String makeKey(DesignNodeType nodeType, String targetId) {
        return String.format("%1$s|%2$s", nodeType.value, targetId);
    }

}