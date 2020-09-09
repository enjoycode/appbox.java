
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.javacs.JavaLanguageServer;
import org.javacs.lsp.InitializeParams;
import org.javacs.lsp.WorkspaceSymbolParams;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class TestLanguageServer {

    @Test
    public void testSymbols() {
        var ls = new JavaLanguageServer(new MockLanguageClient());
        var init= new InitializeParams();
        init.rootUri = Paths.get("src/test/examples/demo1").normalize().toUri();
        ls.initialize(init);
        ls.initialized();

        var symbols = ls.workspaceSymbols(new WorkspaceSymbolParams(""));
        assertNotNull(symbols);
    }

    @Test
    public void testParseWithError() throws IOException {
        var demoFile = Paths.get("src/test/examples/demo1/Service1.java").normalize();
        try {
            var reflectionTypeSolver = new ReflectionTypeSolver();
            var javaParserTypeSolver = new JavaParserTypeSolver(demoFile.getParent());
            var combinedSolver = new CombinedTypeSolver();
            combinedSolver.add(reflectionTypeSolver);
            combinedSolver.add(javaParserTypeSolver);

            var symbolSolver = new JavaSymbolSolver(combinedSolver);
            var parser = new JavaParser();
            parser.getParserConfiguration().setSymbolResolver(symbolSolver);

            //var cu =  StaticJavaParser.parse(demoFile);
            var result = parser.parse(demoFile);
            var cu = result.getResult().get();

            cu.findFirst(NameExpr.class).ifPresent(n -> {
                var type = n.calculateResolvedType();
                System.out.println(type.toString());
            });

            System.out.println("aa");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInjectTypeSolver() {
        CompilationUnit cu = new CompilationUnit();
        var cls = cu.addClass("MyClass").setPublic(true);
        var field = cls.addField("int", "intField", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);

        var reflectionTypeSolver = new ReflectionTypeSolver();
        var symbolSolver = new JavaSymbolSolver(reflectionTypeSolver);
        symbolSolver.inject(cu);

        //var ctx = new CompilationUnitContext(cu, reflectionTypeSolver);

        ResolvedType type1 = field.getVariable(0).getType().resolve();
        var type2 = reflectionTypeSolver.solveType("java.lang.String");
        //var type3 = reflectionTypeSolver.solveType("java");
    }

    @Test
    public void testParseExpression() {
        var src = "class A {\n int sayHello() {\n return 1;\n}\n}";
        var reflectionTypeSolver = new ReflectionTypeSolver();
        var symbolSolver = new JavaSymbolSolver(reflectionTypeSolver);
        var parser = new JavaParser();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);

        var res = parser.parseExpression("Integer");
        var exp = res.getResult().get();

        CompilationUnit cu = new CompilationUnit();
        var cls = cu.addClass("MyClass").setPublic(true);
        exp.setParentNode(cu);

        var type = symbolSolver.calculateType(exp);
        assertNotNull(type);

        //var res = parser.parse(src);
        //var cu = res.getResult().get();
        //var tokenRange = cu.getTokenRange();
        //assertNotNull(cu);
    }

}
