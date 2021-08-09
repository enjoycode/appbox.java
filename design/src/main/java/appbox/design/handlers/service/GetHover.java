package appbox.design.handlers.service;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class GetHover implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var fileName = args.getString();
        var line     = args.getInt() - 1;
        var column   = args.getInt() - 1;

        //TODO:待修改以下查找，暂根据名称找到模型
        var firstDot  = fileName.indexOf('.');
        var lastDot   = fileName.lastIndexOf('.');
        var appName   = fileName.substring(0, firstDot);
        var app       = hub.designTree.findApplicationNodeByName(appName);
        var secondDot = fileName.indexOf('.', firstDot + 1);
        var modelName = fileName.substring(secondDot + 1, lastDot);
        var modelNode = hub.designTree.findModelNodeByName(app.model.id(), ModelType.Service, modelName);
        var modelId   = modelNode.model().id();
        var doc       = hub.typeSystem.javaLanguageServer.findOpenedDocument(modelId);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", fileName);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        var contents = hub.typeSystem.javaLanguageServer.hover(doc, line, column);
        if (contents == null || contents.length == 0)
            return CompletableFuture.completedFuture(null);

        var res = new HoverInfo();
        res.contents = new MarkdownString[contents.length];
        for (int i = 0; i < contents.length; i++) {
            res.contents[i]       = new MarkdownString();
            res.contents[i].value = contents[i];
        }

        return CompletableFuture.completedFuture(new JsonResult(res));
    }

    static class MarkdownString {
        public String value;
    }

    static class HoverInfo {
        public MarkdownString[] contents;
    }
}
