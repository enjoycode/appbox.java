import com.google.dart.server.*;
import com.google.dart.server.internal.remote.DebugPrintStream;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;
import com.google.dart.server.internal.remote.StdioServerSocket;
import org.dartlang.analysis.server.protocol.*;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestDartServer {

    @Test
    public void test1() throws Exception {
        final String sdkPath            = "/home/rick/snap/flutter/common/flutter/";
        final String runtimePath        = sdkPath + "bin/dart";
        final String analysisServerPath = sdkPath + "bin/cache/dart-sdk/bin/snapshots/analysis_server.dart.snapshot";

        final DebugPrintStream debugStream = null; //System.out::println;

        final StdioServerSocket serverSocket =
                new StdioServerSocket(runtimePath, null, analysisServerPath,
                        null, debugStream);
        serverSocket.setClientId("VSCODE-REMOTE");
        serverSocket.setClientVersion("1.0.0");

        final RemoteAnalysisServerImpl server = new RemoteAnalysisServerImpl(serverSocket);
        server.start();
        server.addAnalysisServerListener(createServerListener());

        //server.addRequestListener((json) -> {
        //    System.out.println("Request: " + json);
        //});
        //server.addResponseListener((json) -> {
        //    System.out.println("Response: " + json);
        //});
        //server.addStatusListener((isAlive) -> {
        //    System.out.println("Status: " + isAlive);
        //});

        //server.server_setSubscriptions(List.of("LOG"/*, "STATUS"*/));

        final String mainFile     = "/home/rick/Desktop/template/lib/main.dart";
        final String pubspecFile  = "/home/rick/Desktop/template/pubspec.yaml";
        var          visibleFiles = List.of(mainFile);

        //var service = new HashMap<String, List<String>>() {{
        //    put("FOLDING", visibleFiles);
        //    put("OUTLINE", visibleFiles);
        //}};
        //server.analysis_setSubscriptions(service);

        server.completion_setSubscriptions(List.of("AVAILABLE_SUGGESTION_SETS"));
        server.analysis_setPriorityFiles(visibleFiles);

        //命令测试
        server.analysis_setAnalysisRoots(List.of("/home/rick/Desktop/template"), null, null);
        Thread.sleep(2000);

        server.completion_getSuggestions(mainFile, 1, new GetSuggestionsConsumer() {
            @Override
            public void computedCompletionId(String completionId) {
                System.out.println("Completion: " + completionId);
            }

            @Override
            public void onError(RequestError requestError) {
                System.out.println("Completion error");
            }
        });

        Thread.sleep(5000);
        server.server_shutdown();
        //Thread.sleep(1000);
    }

    private static AnalysisServerListener createServerListener() {
        return new AnalysisServerListenerAdapter() {
            @Override
            public void serverConnected(String version) {
                System.out.println("Server connected: " + version);
            }

            @Override
            public void serverError(boolean isFatal, String message, String stackTrace) {
                System.out.println("Server error: " + message);
            }

            @Override
            public void computedErrors(String file, List<AnalysisError> errors) {
                if (!errors.isEmpty()) {
                    System.out.println("File error: " + file);
                }
            }

            @Override
            public void computedCompletion(String completionId, int replacementOffset,
                                           int replacementLength, List<CompletionSuggestion> completions,
                                           List<IncludedSuggestionSet> includedSuggestionSets,
                                           List<String> includedElementKinds,
                                           List<IncludedSuggestionRelevanceTag> includedSuggestionRelevanceTags,
                                           boolean isLast, @Nullable String libraryFile) {
                System.out.println("Completion result: " + completionId + " isLast=" + isLast);
                for (int i = 0; i < completions.size(); i++) {
                    System.out.println("[" + i + "]: \"" + completions.get(i).getCompletion() + "\"");
                }
            }

            @Override
            public void computedAvailableSuggestions(@Nonnull List<AvailableSuggestionSet> changed, @Nonnull int[] removed) {
                //System.out.println("AvailableSuggestionSet:");
                //for (var set : changed) {
                //    System.out.println("SuggestionSet id: " + set.getId() + " uri:" + set.getUri());
                //    for (var item : set.getItems()) {
                //        System.out.println(item.getLabel());
                //    }
                //}
            }

            @Override
            public void computedExistingImports(String file, ExistingImports existingImports) {
                System.out.println(existingImports);
            }
        };
    }

}
