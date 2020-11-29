package appbox.design.services;

import appbox.design.DesignHub;
import appbox.design.jdt.JavaBuilderWrapper;
import appbox.design.services.code.ServiceCodeGenerator;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.IService;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jface.text.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class PublishService {

    private PublishService() {}

    /**
     * 发布或调试时编译服务模型
     * @return 返回的是已经压缩过的
     */
    public static byte[] compileService(DesignHub hub, ServiceModel model, String debugFolder) throws Exception {

        //获取对应的虚拟文件
        var designNode = hub.designTree.findModelNode(ModelType.Service, model.id());
        var appName    = designNode.appNode.model.name();
        var vfile      = hub.typeSystem.findFileForServiceModel(appName, model.name());
        var cu         = JDTUtils.resolveCompilationUnit(vfile);

        //TODO:先检测虚拟代码错误

        //TODO:开始转换编译服务模型的运行时代码
        var astParser = ASTParser.newParser(AST.JLS15);
        astParser.setSource(cu);
        astParser.setResolveBindings(true);
        //astParser.setStatementsRecovery(true);
        var astNode    = astParser.createAST(null);
        var astRewrite = ASTRewrite.create(astNode.getAST());

        var serviceCodeGenerator = new ServiceCodeGenerator(hub, appName, model, astRewrite);
        astNode.accept(serviceCodeGenerator);
        serviceCodeGenerator.finish();

        var edits  = astRewrite.rewriteAST();
        var newdoc = new Document(cu.getSource());
        edits.apply(newdoc);

        var runtimeCode       = newdoc.get();
        var runtimeCodeStream = new ByteArrayInputStream(runtimeCode.getBytes(StandardCharsets.UTF_8));

        //生成运行时临时Project并进行编译
        var libAppBoxCorePath = new Path(IService.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        var libs = new IClasspathEntry[]{
                JavaCore.newLibraryEntry(libAppBoxCorePath, null, null)
        };
        var runtimeProject =
                hub.typeSystem.languageServer.createProject(
                        "runtime_" + Long.toUnsignedString(model.id()), libs);
        var runtimeFile = runtimeProject.getFile(vfile.getName());
        runtimeFile.create(runtimeCodeStream, true, null);

        var config  = new BuildConfiguration(runtimeProject);
        var builder = new JavaBuilderWrapper(config);
        builder.build();

        throw new RuntimeException("未实现");
    }

}