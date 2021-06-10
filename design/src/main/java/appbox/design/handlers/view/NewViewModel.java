package appbox.design.handlers.view;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.handlers.ModelCreator;
import appbox.design.tree.DesignNodeType;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public class NewViewModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();
        var type             = (byte) args.getInt();

        Object[] initCodes = null;
        if (type == ViewModel.TYPE_VUE) {
            var templateCode = "<div>Hello Future!</div>";
            var scriptCode   = String.format("@Component\nexport default class %s extends Vue {\n\n}\n", name);
            initCodes = new Object[]{templateCode, scriptCode, "", ""};
        } else if (type == ViewModel.TYPE_FLUTTER) {
            initCodes = new Object[]{"", buildFlutterTemplate(name), "", ""};
        }

        return ModelCreator.create(hub, ModelType.View, id -> new ViewModel(id, name, type)
                , selectedNodeType, selectedNodeId, name, initCodes);
    }

    private String buildFlutterTemplate(String className) {
        var controllerName = className + "Controller";
        var sb             = new StringBuilder();
        sb.append("import 'package:flutter/material.dart';\n");
        sb.append("import 'package:get/get.dart';\n\n");
        sb.append("class ").append(controllerName).append(" extends GetxController {\n");
        sb.append("  final data = 'Hello Future!'.obs;\n");
        sb.append("}\n\n");

        sb.append("class ").append(className).append(" extends StatelessWidget {\n");
        sb.append("  final controller = Get.put(").append(controllerName).append("());\n\n");
        sb.append("  @override\n");
        sb.append("  Widget build(BuildContext context) {\n");
        sb.append("    return Obx(() => Text(controller.data.value));\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }
}
