import com.intellij.core.CoreJavaFileManager;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiCachedValuesFactory;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.CachedValuesManagerImpl;
import com.intellij.util.messages.ListenerDescriptor;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusOwner;
import com.intellij.util.messages.impl.MessageBusFactoryImpl;
import com.intellij.util.pico.DefaultPicoContainer;

public final class TestProject extends UserDataHolderBase implements Project, MessageBusOwner {

    public final  CoreJavaFileManager fileManager;
    public final  PsiManager          psiManager;
    private final CachedValuesManager cachedValuesManager;
    private final PsiDocumentManager  psiDocumentManager;
    private final MessageBus          messageBus = MessageBusFactoryImpl.createRootBus(this);

    public TestProject() {
        psiManager          = new TestPsiManager(this); //new PsiManagerImpl(this);
        fileManager         = new CoreJavaFileManager(psiManager);
        cachedValuesManager = new CachedValuesManagerImpl(this, new PsiCachedValuesFactory(this));
        psiDocumentManager  = new TestPsiDocumentManager(this);
    }

    @Override
    public VirtualFile getProjectFile() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getLocationHash() {
        return "dummy";
    }

    @Override
    public String getProjectFilePath() {
        return null;
    }

    @Override
    public VirtualFile getWorkspaceFile() {
        return null;
    }

    @Override
    public VirtualFile getBaseDir() {
        return null;
    }

    @Override
    public String getBasePath() {
        return null;
    }

    @Override
    public void save() {
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        var simpleName = serviceClass.getSimpleName();
        if (simpleName.equals("PsiManager")) {
            return (T) psiManager;
        } else if (simpleName.equals("CachedValuesManager")) {
            return (T) cachedValuesManager;
        } else if (simpleName.equals("PsiDocumentManager")) {
            return (T) psiDocumentManager;
        }
        return null;
    }

    @Override
    public <T> T getComponent(Class<T> interfaceClass) {
        return null;
    }

    @Override
    public DefaultPicoContainer getPicoContainer() {
        throw new UnsupportedOperationException("getPicoContainer is not implement in : " + getClass());
    }

    @Override
    public ExtensionsArea getExtensionArea() {
        throw new UnsupportedOperationException("getExtensionArea is not implement in : " + getClass());
    }

    @Override
    public Object createListener(ListenerDescriptor listenerDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public Condition<?> getDisposed() {
        return o -> isDisposed();
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public MessageBus getMessageBus() {
        return messageBus;
    }

    @Override
    public void dispose() {
    }

}
