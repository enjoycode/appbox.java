package appbox.runtime;

import appbox.data.TreeNodePath;

import java.util.UUID;

/** 用户会话信息 */
public interface IUserSession {

    String name();

    /**
     * 是否外部用户
     */
    boolean isExternal(); //TODO:考虑改为会话类型，如：内部用户、外部用户、系统内置用户

    /**
     * 附加会话标记，如SaaS外部用户的租户ID等
     */
    String tag();

    /**
     * 会话标识号，仅用于服务端会话管理
     */
    long sessionId();

    /**
     * 当前会话用户的组织路径深度, 注意：外部会话包含External一级
     */
    int levels();

    /**
     * 获了层级信息,注意0为最后一级
     */
    TreeNodePath.TreeNodeInfo getAt(int level);

    /**
     * 获取最后一级的组织单元标识, 如果是外部用户则返回上一级WorkGroup的组织单元标识
     */
    UUID leafOrgUnitId();

    /**
     * 获取内部会话对应的员工标识，如果是外部用户则返回Empty
     */
    UUID emploeeId();

    /**
     * 获取外部会话对应的外部用户标识，如果是内部用户则返回Empty
     */
    UUID externalId();

}
