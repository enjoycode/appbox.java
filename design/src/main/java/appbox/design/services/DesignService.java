package appbox.design.services;

import appbox.design.IDeveloperSession;
import appbox.design.handlers.*;
import appbox.design.handlers.code.ChangeBuffer;
import appbox.design.handlers.code.CheckCode;
import appbox.design.handlers.code.GetCompletion;
import appbox.design.handlers.entity.*;
import appbox.design.handlers.service.*;
import appbox.design.handlers.setting.GetAppSettings;
import appbox.design.handlers.setting.SaveAppSettings;
import appbox.design.handlers.store.DeleteBlobObject;
import appbox.design.handlers.store.GetBlobObjects;
import appbox.design.handlers.store.NewDataStore;
import appbox.design.handlers.store.SaveDataStore;
import appbox.design.handlers.tool.GetAssembly;
import appbox.design.handlers.tree.Checkout;
import appbox.design.handlers.tree.DeleteNode;
import appbox.design.handlers.tree.DragDropNode;
import appbox.design.handlers.tree.LoadDesignTree;
import appbox.design.handlers.view.LoadView;
import appbox.design.handlers.view.NewViewModel;
import appbox.design.handlers.view.OpenViewModel;
import appbox.runtime.IService;
import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public final class DesignService implements IService {

    private final HashMap<CharSequence, IDesignHandler> handlers = new HashMap<>() {{
        put("LoadDesignTree", new LoadDesignTree());
        put("Checkout", new Checkout());
        put("ChangeBuffer", new ChangeBuffer());
        put("GetCompletion", new GetCompletion());
        put("CheckCode", new CheckCode());
        put("CloseDesigner", new CloseDesigner());
        put("SaveModel", new SaveModel());
        put("DeleteNode", new DeleteNode());
        put("GetPendingChanges", new GetPendingChanges());
        put("Publish", new Publish());
        put("NewApplication", new NewApplication());
        put("NewFolder", new NewFolder());
        put("GetAssembly", new GetAssembly());
        put("DragDropNode", new DragDropNode());
        //----DataStore----
        put("NewDataStore", new NewDataStore());
        put("SaveDataStore", new SaveDataStore());
        put("GetBlobObjects", new GetBlobObjects());
        put("DeleteBlobObject", new DeleteBlobObject());
        //----Entity----
        put("NewEntityModel", new NewEntityModel());
        put("NewEntityMember", new NewEntityMember());
        put("ChangeEntity", new ChangeEntity());
        put("DeleteEntityMember", new DeleteEntityMember());
        put("GetEntityModel", new GetEntityModel());
        put("GetEntityRefModels", new GetEntityRefModels());
        put("LoadEntityData", new LoadEntityData());
        put("GenEntityDeclare", new GenEntityDeclare());
        //----Service----
        put("OpenServiceModel", new OpenServiceModel());
        put("GenServiceDeclare", new GenServiceDeclare());
        put("GetServiceMethod", new GetServiceMethod());
        put("GetMethodInfo", new GetMethodInfo());
        put("NewServiceModel", new NewServiceModel());
        put("GetHover", new GetHover());
        put("StartDebugging", new StartDebugging());
        put("ContinueBreakpoint", new ContinueDebug()); //TODO:改名，暂兼容旧名称
        //----view----
        put("NewViewModel", new NewViewModel());
        put("OpenViewModel", new OpenViewModel());
        put("LoadView", new LoadView());
        //----Permission----
        put("NewPermissionModel", new NewPermissionModel());
        //----Settings----
        put("GetAppSettings", new GetAppSettings());
        put("SaveAppSettings", new SaveAppSettings());
    }};

    @Override
    public CompletableFuture<Object> invokeAsync(CharSequence method, InvokeArgs args) {
        if (!(RuntimeContext.current().currentSession() instanceof IDeveloperSession)) {
            return CompletableFuture.failedFuture(new RuntimeException("Must login as a developer"));
        }

        var session   = (IDeveloperSession) RuntimeContext.current().currentSession();
        var designHub = session.getDesignHub();
        if (designHub == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Can't get DesignContext"));
        }

        var handler = handlers.get(method);
        if (handler == null) {
            return CompletableFuture.failedFuture(new RuntimeException("Unknown design request: " + method));
        }

        try {
            return handler.handle(designHub, args);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
