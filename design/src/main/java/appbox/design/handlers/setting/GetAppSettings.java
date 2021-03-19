package appbox.design.handlers.setting;

import appbox.data.EntityId;
import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.entities.Settings;
import appbox.runtime.InvokeArgs;
import appbox.store.query.IndexGet;

import java.util.concurrent.CompletableFuture;

/** 根据App名称及配置名称获取配置值 */
public final class GetAppSettings implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var appName      = args.getString(); //可能为null
        var settingsName = args.getString();

        if (settingsName == null || settingsName.isEmpty())
            throw new RuntimeException("Settings name is empty");

        int appId = 0;
        if (appName != null && !appName.isEmpty()) {
            var appNode = hub.designTree.findApplicationNodeByName(appName);
            if (appNode == null)
                throw new RuntimeException("Can't find Application");
            appId = appNode.model.id();
        }

        var q = new IndexGet<>(Settings.UI_Settings.class);
        q.where(Settings.APPID, appId);
        q.where(Settings.USERID, EntityId.empty()); //仅全局非用户配置
        q.where(Settings.NAME, settingsName);

        return q.toEntityAsync().thenApply(settings -> {
            //TODO: 根据类型转换，暂只处理JSON
            if (settings.getType().equals("Json")) {
                return new JsonResult(settings.getValue(), true);
            } else {
                throw new RuntimeException("未实现");
            }
        });
    }

}
