package appbox.design.services.code;

import appbox.runtime.InvokeArgs;
import appbox.runtime.RuntimeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

public final class InvokeServiceInterceptor implements IMethodInterceptor {

    //sys.services.HelloService.sayHello("Rick")
    // ->
    //appbox.runtime.RuntimeContext.invokeAsync("sys.HelloService.sayHello",
    //      InvokeArgs.make().add("Rick").done()).thenApply(r->(String)r);

    @Override
    public boolean visit(MethodInvocation node, ServiceCodeGenerator generator) {
        var serviceType = node.getExpression().resolveTypeBinding();
        var serviceMethod = serviceType.getPackage().getNameComponents()[0]
            + "." + serviceType.getName() + "." + node.getName();
        var arg1 = generator.ast.newStringLiteral();
        arg1.setLiteralValue(serviceMethod);

        Object arg2 = null;
        if (node.arguments().size() > 0) {
            var argsName = generator.ast.newName(InvokeArgs.class.getName());
            var makeMethod = generator.ast.newMethodInvocation();
            makeMethod.setName(generator.ast.newSimpleName("make"));
            makeMethod.setExpression(argsName);

            //需要转换参数后再重新加入
            for(var arg : node.arguments()) {
                ((ASTNode)arg).accept(generator);
            }

            MethodInvocation addMethod = makeMethod;
            var listRewrite = generator.astRewrite.getListRewrite(node, MethodInvocation.ARGUMENTS_PROPERTY);
            for(var arg : listRewrite.getRewrittenList()) {
                var argNode = (ASTNode)arg;
                var tempMethod = generator.ast.newMethodInvocation();
                tempMethod.setName(generator.ast.newSimpleName("add"));
                tempMethod.setExpression(addMethod);
                if (argNode.getParent() != null)
                    argNode = ASTNode.copySubtree(generator.ast, argNode);
                tempMethod.arguments().add(argNode);
                addMethod = tempMethod;
            }

            var doneMethod = generator.ast.newMethodInvocation();
            doneMethod.setName(generator.ast.newSimpleName("done"));
            doneMethod.setExpression(addMethod);
            arg2 = doneMethod;
        } else {
            arg2 = generator.ast.newNullLiteral();
        }

        var invokeMethod = generator.ast.newMethodInvocation();
        invokeMethod.setName(generator.ast.newSimpleName("invokeAsync"));
        invokeMethod.setExpression(generator.ast.newName(RuntimeContext.class.getName()));
        invokeMethod.arguments().add(arg1);
        invokeMethod.arguments().add(arg2);

        //转换服务调用的返回类型
        var castMethod = invokeMethod;
        var returnType = node.resolveMethodBinding().getReturnType().getTypeArguments()[0];
        if (!returnType.getName().equals("Void")) {
            castMethod = generator.makeFutureCast(
                    invokeMethod, generator.ast.newSimpleType(generator.ast.newName(returnType.getName())));
        }

        generator.astRewrite.replace(node, castMethod, null);

        return false;
    }
}
