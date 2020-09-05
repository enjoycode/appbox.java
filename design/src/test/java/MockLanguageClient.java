import com.google.gson.JsonElement;
import org.javacs.lsp.LanguageClient;
import org.javacs.lsp.PublishDiagnosticsParams;
import org.javacs.lsp.ShowMessageParams;

public class MockLanguageClient implements LanguageClient {
    @Override
    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {
        System.out.printf("MockClient.publishDiagnostics: %s%n", publishDiagnosticsParams.uri.toString());
    }

    @Override
    public void showMessage(ShowMessageParams showMessageParams) {
        System.out.printf("MockClient.showMeesage: %d %s%n",
                showMessageParams.type, showMessageParams.message);
    }

    @Override
    public void registerCapability(String s, JsonElement jsonElement) {
        System.out.printf("MockClient.registerCapability: %s: %s%n", s, jsonElement.toString());
    }

    @Override
    public void customNotification(String s, JsonElement jsonElement) {
        System.out.printf("MockClient.customNotification: %s: %s%n", s, jsonElement.toString());
    }
}
