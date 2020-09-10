package org.javacs.completion;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;
import org.javacs.FindHelper;
import org.javacs.MarkdownHelper;
import org.javacs.hover.ShortTypePrinter;
import org.javacs.lsp.ParameterInformation;
import org.javacs.lsp.SignatureHelp;
import org.javacs.lsp.SignatureInformation;

public class SignatureProvider {

    private final CompilerProvider compiler;

    public static final SignatureHelp NOT_SUPPORTED = new SignatureHelp(List.of(), -1, -1);

    public SignatureProvider(CompilerProvider compiler) {
        this.compiler = compiler;
    }

    public SignatureHelp signatureHelp(Path file, int line, int column) {
        // TODO prune
        try (var task = compiler.compile(file)) {
            var cursor = task.root().getLineMap().getPosition(line, column);
            var path = new FindInvocationAt(task.task).scan(task.root(), cursor);
            if (path == null) return NOT_SUPPORTED;
            if (path.getLeaf() instanceof MethodInvocationTree) {
                var invoke = (MethodInvocationTree) path.getLeaf();
                var overloads = methodOverloads(task, invoke);
                var signatures = new ArrayList<SignatureInformation>();
                for (var method : overloads) {
                    var info = info(method);
                    addSourceInfo(task, method, info);
                    addFancyLabel(info);
                    signatures.add(info);
                }
                var activeSignature = activeSignature(task, path, invoke.getArguments(), overloads);
                var activeParameter = activeParameter(task, invoke.getArguments(), cursor);
                return new SignatureHelp(signatures, activeSignature, activeParameter);
            }
            if (path.getLeaf() instanceof NewClassTree) {
                var invoke = (NewClassTree) path.getLeaf();
                var overloads = constructorOverloads(task, invoke);
                var signatures = new ArrayList<SignatureInformation>();
                for (var method : overloads) {
                    var info = info(method);
                    addSourceInfo(task, method, info);
                    addFancyLabel(info);
                    signatures.add(info);
                }
                var activeSignature = activeSignature(task, path, invoke.getArguments(), overloads);
                var activeParameter = activeParameter(task, invoke.getArguments(), cursor);
                return new SignatureHelp(signatures, activeSignature, activeParameter);
            }
            return NOT_SUPPORTED;
        }
    }

    private List<ExecutableElement> methodOverloads(CompileTask task, MethodInvocationTree method) {
        if (method.getMethodSelect() instanceof IdentifierTree) {
            var id = (IdentifierTree) method.getMethodSelect();
            return scopeOverloads(task, id);
        }
        if (method.getMethodSelect() instanceof MemberSelectTree) {
            var select = (MemberSelectTree) method.getMethodSelect();
            return memberOverloads(task, select);
        }
        throw new RuntimeException(method.getMethodSelect().toString());
    }

    private List<ExecutableElement> scopeOverloads(CompileTask task, IdentifierTree method) {
        var trees = Trees.instance(task.task);
        var path = trees.getPath(task.root(), method);
        var scope = trees.getScope(path);
        var list = new ArrayList<ExecutableElement>();
        Predicate<CharSequence> filter = name -> method.getName().contentEquals(name);
        // TODO add static imports
        for (var member : ScopeHelper.scopeMembers(task, scope, filter)) {
            if (member.getKind() == ElementKind.METHOD) {
                list.add((ExecutableElement) member);
            }
        }
        return list;
    }

    private List<ExecutableElement> memberOverloads(CompileTask task, MemberSelectTree method) {
        var trees = Trees.instance(task.task);
        var path = trees.getPath(task.root(), method.getExpression());
        var isStatic = trees.getElement(path) instanceof TypeElement;
        var scope = trees.getScope(path);
        var type = typeElement(trees.getTypeMirror(path));
        if (type == null) return List.of();
        var list = new ArrayList<ExecutableElement>();
        for (var member : task.task.getElements().getAllMembers(type)) {
            if (member.getKind() != ElementKind.METHOD) continue;
            if (!member.getSimpleName().contentEquals(method.getIdentifier())) continue;
            if (isStatic != member.getModifiers().contains(Modifier.STATIC)) continue;
            if (!trees.isAccessible(scope, member, (DeclaredType) type.asType())) continue;
            list.add((ExecutableElement) member);
        }
        return list;
    }

    private TypeElement typeElement(TypeMirror type) {
        if (type instanceof DeclaredType) {
            var declared = (DeclaredType) type;
            return (TypeElement) declared.asElement();
        }
        if (type instanceof TypeVariable) {
            var variable = (TypeVariable) type;
            return typeElement(variable.getUpperBound());
        }
        return null;
    }

    private List<ExecutableElement> constructorOverloads(CompileTask task, NewClassTree method) {
        var trees = Trees.instance(task.task);
        var path = trees.getPath(task.root(), method.getIdentifier());
        var scope = trees.getScope(path);
        var type = (TypeElement) trees.getElement(path);
        var list = new ArrayList<ExecutableElement>();
        for (var member : task.task.getElements().getAllMembers(type)) {
            if (member.getKind() != ElementKind.CONSTRUCTOR) continue;
            if (!trees.isAccessible(scope, member, (DeclaredType) type.asType())) continue;
            list.add((ExecutableElement) member);
        }
        return list;
    }

    private SignatureInformation info(ExecutableElement method) {
        var info = new SignatureInformation();
        info.label = method.getSimpleName().toString();
        if (method.getKind() == ElementKind.CONSTRUCTOR) {
            info.label = method.getEnclosingElement().getSimpleName().toString();
        }
        info.parameters = parameters(method);
        return info;
    }

    private List<ParameterInformation> parameters(ExecutableElement method) {
        var list = new ArrayList<ParameterInformation>();
        for (var p : method.getParameters()) {
            list.add(parameter(p));
        }
        return list;
    }

    private ParameterInformation parameter(VariableElement p) {
        var info = new ParameterInformation();
        info.label = ShortTypePrinter.NO_PACKAGE.print(p.asType());
        return info;
    }

    private void addSourceInfo(CompileTask task, ExecutableElement method, SignatureInformation info) {
        var type = (TypeElement) method.getEnclosingElement();
        var className = type.getQualifiedName().toString();
        var methodName = method.getSimpleName().toString();
        var erasedParameterTypes = FindHelper.erasedParameterTypes(task, method);
        var file = compiler.findAnywhere(className);
        if (file.isEmpty()) return;
        var parse = compiler.parse(file.get());
        var source = FindHelper.findMethod(parse, className, methodName, erasedParameterTypes);
        var path = Trees.instance(task.task).getPath(parse.root, source);
        var docTree = DocTrees.instance(task.task).getDocCommentTree(path);
        if (docTree != null) {
            info.documentation = MarkdownHelper.asMarkupContent(docTree);
        }
        info.parameters = parametersFromSource(source);
    }

    private void addFancyLabel(SignatureInformation info) {
        var join = new StringJoiner(", ");
        for (var p : info.parameters) {
            join.add(p.label);
        }
        info.label = info.label + "(" + join + ")";
    }

    private List<ParameterInformation> parametersFromSource(MethodTree source) {
        var list = new ArrayList<ParameterInformation>();
        for (var p : source.getParameters()) {
            var info = new ParameterInformation();
            info.label = p.getType() + " " + p.getName();
            list.add(info);
        }
        return list;
    }

    private int activeParameter(CompileTask task, List<? extends ExpressionTree> arguments, long cursor) {
        var pos = Trees.instance(task.task).getSourcePositions();
        ;
        var root = task.root();
        for (var i = 0; i < arguments.size(); i++) {
            var end = pos.getEndPosition(root, arguments.get(i));
            if (cursor <= end) {
                return i;
            }
        }
        return arguments.size();
    }

    private int activeSignature(
            CompileTask task,
            TreePath invocation,
            List<? extends ExpressionTree> arguments,
            List<ExecutableElement> overloads) {
        for (var i = 0; i < overloads.size(); i++) {
            if (isCompatible(task, invocation, arguments, overloads.get(i))) {
                return i;
            }
        }
        return 0;
    }

    private boolean isCompatible(
            CompileTask task,
            TreePath invocation,
            List<? extends ExpressionTree> arguments,
            ExecutableElement overload) {
        if (arguments.size() > overload.getParameters().size()) return false;
        for (var i = 0; i < arguments.size(); i++) {
            var argument = arguments.get(i);
            var argumentType = Trees.instance(task.task).getTypeMirror(new TreePath(invocation, argument));
            var parameterType = overload.getParameters().get(i).asType();
            if (!isCompatible(task, argumentType, parameterType)) return false;
        }
        return true;
    }

    private boolean isCompatible(CompileTask task, TypeMirror argument, TypeMirror parameter) {
        if (argument instanceof ErrorType) return true;
        if (argument instanceof PrimitiveType) {
            argument = task.task.getTypes().boxedClass((PrimitiveType) argument).asType();
        }
        if (parameter instanceof PrimitiveType) {
            parameter = task.task.getTypes().boxedClass((PrimitiveType) parameter).asType();
        }
        if (argument instanceof ArrayType) {
            if (!(parameter instanceof ArrayType)) return false;
            var argumentA = (ArrayType) argument;
            var parameterA = (ArrayType) parameter;
            return isCompatible(task, argumentA.getComponentType(), parameterA.getComponentType());
        }
        if (argument instanceof DeclaredType) {
            if (!(parameter instanceof DeclaredType)) return false;
            argument = task.task.getTypes().erasure(argument);
            parameter = task.task.getTypes().erasure(parameter);
            return argument.toString().equals(parameter.toString());
        }
        return true;
    }
}
