import org.javacs.JavaLanguageServer;
import org.javacs.lsp.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class TestLanguageServer {

    @Test
    public void testSymbols() {
        var ls   = new JavaLanguageServer(new MockLanguageClient());
        var init = new InitializeParams();
        init.rootUri = Paths.get("src/test/examples/demo1").normalize().toUri();
        ls.initialize(init);
        ls.initialized();
        assertNotNull(ls);

        //var symbols = ls.workspaceSymbols(new WorkspaceSymbolParams(""));
        //assertNotNull(symbols);

        var uri = Paths.get("src/test/examples/demo1/Service1.java").normalize().toUri();
        var position = new TextDocumentPositionParams(
                new TextDocumentIdentifier(uri),
                new Position(5 - 1, 12 - 1));
        var completions = ls.completion(position);
        assertNotNull(completions);
    }

}
