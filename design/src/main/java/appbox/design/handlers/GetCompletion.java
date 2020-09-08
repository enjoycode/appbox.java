package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.runtime.InvokeArg;
import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GetCompletion implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var type           = args.get(0).getInt();
        var fileName       = args.get(1).getString();
        var line           = args.get(2).getInt();
        var column         = args.get(3).getInt();
        var wordToComplete = args.get(4).getString();

        Log.debug(String.format("GetCompletion: %d %s %d-%d %s", type, fileName, line, column, wordToComplete));
        var doc = hub.typeSystem.opendDocs.get(fileName);
        var parser = new JavaParser();
        var res = parser.parse(doc.sourceCode.toString());
        var nodes = res.getResult().get().findAll(Node.class, n -> {
            return n.getRange().get().contains(new Position(line, column));
            //return true;
        });

        return CompletableFuture.completedFuture(null);
    }
}
