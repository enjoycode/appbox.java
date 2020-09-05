
import org.javacs.JavaLanguageServer;
import org.javacs.lsp.InitializeParams;
import org.javacs.lsp.TextDocumentPositionParams;
import org.javacs.lsp.WorkspaceSymbolParams;
import org.junit.jupiter.api.Test;

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

    //private String symbolAt(String file, int line, int character) {
    //    var pos =
    //            new TextDocumentPositionParams(
    //                    new TextDocumentIdentifier(FindResource.uri(file)), new Position(line - 1, character - 1));
    //    var result = new StringJoiner("\n");
    //    for (var h : server.hover(pos).get().contents) {
    //        result.add(h.value);
    //    }
    //    return result.toString();
    //}
}
