package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.runtime.InvokeArg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GetCompletion implements IRequestHandler {
    public static final class AutoCompleteItem {
        public String detail;           //returnType + displayText
        public String documentation;
        public int    kind;
        public String insertText;
        public String label;            //displayText
    }

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var type           = args.get(0).getInt();
        var fileName       = args.get(1).getString();
        var line           = args.get(2).getInt() - 1; //注意：前端值需要-1
        var column         = args.get(3).getInt() - 1; //注意：前端值需要-1
        var wordToComplete = args.get(4).getString();

        Log.debug(String.format("GetCompletion: %d %s %d-%d %s", type, fileName, line, column, wordToComplete));
        var doc = hub.typeSystem.opendDocs.get(fileName);
        if (doc == null) {
            var error = String.format("Can't find opened ServiceModel: %s", fileName);
            return CompletableFuture.failedFuture(new Exception(error));
        }

        //TODO:判断上一行结尾 ; { }
        //TODO:以下测试
        var list = new ArrayList<AutoCompleteItem>();

        var lineText = doc.sourceText.getLine(line);
        Log.debug(lineText);
        //if (lineText.endsWith(".")) {
        //    var reflectionTypeSolver = new ReflectionTypeSolver();
        //    var symbolSolver         = new JavaSymbolSolver(reflectionTypeSolver);
        //    var parser               = new JavaParser();
        //    parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        //
        //    var cu = new CompilationUnit();
        //    symbolSolver.inject(cu);
        //    var res = parser.parseExpression(lineText.substring(0, column - 1));
        //    var exp = res.getResult().get();
        //    exp.setParentNode(cu);
        //    var resolvedType = exp.calculateResolvedType();
        //
        //    if (resolvedType.isReferenceType()) {
        //        var rtype   = resolvedType.asReferenceType();
        //        var methods = rtype.getAllMethods();
        //        for (var method : methods) {
        //            var item = new AutoCompleteItem();
        //            item.label         = method.getName();
        //            item.insertText    = method.getName();
        //            item.documentation = method.getQualifiedSignature();
        //            item.detail        = method.getReturnType().describe() + " " + method.getName();
        //            item.kind          = 1; //Kind.Method
        //            list.add(item);
        //        }
        //    }
        //
        //}

        return CompletableFuture.completedFuture(new JsonResult(list));
    }
}
