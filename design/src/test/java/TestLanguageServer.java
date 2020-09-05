
import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.javacs.JavaLanguageServer;
import org.javacs.lsp.InitializeParams;
import org.javacs.lsp.TextDocumentPositionParams;
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
            var ts = new ReflectionTypeSolver();
            var ss = new JavaSymbolSolver(ts);
            var parser = new JavaParser();
            parser.getParserConfiguration().setSymbolResolver(ss);


            //var cu =  StaticJavaParser.parse(demoFile);
            var result = parser.parse(demoFile);
            var cu = result.getResult().get();
            //var type = JavaParserFacade.get(ts).getType(cu.getChildNodes().get(0).getChildNodes().get(2));
            //assertNotNull(cu);
            //
            //var ts = new ReflectionTypeSolver();
            //var ss = new JavaSymbolSolver(ts);
            //ss.inject(cu);
            System.out.println("aa");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
