import com.google.gson.JsonElement;
import org.javacs.lsp.LanguageClient;
import org.javacs.lsp.PublishDiagnosticsParams;
import org.javacs.lsp.ShowMessageParams;

public class MockLanguageClient implements LanguageClient {
    @Override
    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {

    }

    @Override
    public void showMessage(ShowMessageParams showMessageParams) {

    }

    @Override
    public void registerCapability(String s, JsonElement jsonElement) {

    }

    @Override
    public void customNotification(String s, JsonElement jsonElement) {

    }
}
