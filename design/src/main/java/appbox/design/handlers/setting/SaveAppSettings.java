package appbox.design.handlers.setting;

import appbox.data.EntityId;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.entities.Settings;
import appbox.runtime.InvokeArgs;
import appbox.store.EntityStore;
import appbox.store.query.IndexGet;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/** 保存配置 */
public final class SaveAppSettings implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var appName      = args.getString(); //可能为null
        var settingsName = args.getString();
        var type         = args.getString();
        var valueJson    = args.getString();

        if (settingsName == null || settingsName.isEmpty()
                || type == null || type.isEmpty()
                || valueJson == null || valueJson.isEmpty())
            throw new RuntimeException("Parameter error");

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
        return q.toEntityAsync().thenCompose(settings -> {
            if (settings == null)
                throw new RuntimeException("Can't find applition settings");

            //TODO:根据类型设置值，暂只处理Json
            if (type.equals("Json")) {
                var jsonData = valueJson.getBytes(StandardCharsets.UTF_8);
                settings.setValue(jsonData);
            } else {
                throw new RuntimeException("未实现");
            }

            return EntityStore.updateEntityAsync(settings);
        }).thenApply(r -> null);
    }

}
