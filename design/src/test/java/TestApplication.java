import com.intellij.core.CoreEncodingRegistry;
import com.intellij.mock.MockFileDocumentManagerImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.util.messages.MessageBus;
import org.picocontainer.PicoContainer;

import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class TestApplication extends UserDataHolderBase implements Application {
    private final Disposable          _lastDisposable      = Disposer.newDisposable(); // will be disposed last
    private       FileDocumentManager _fileDocumentManager =
            new MockFileDocumentManagerImpl(charSequence -> new DocumentImpl(charSequence), null);
    private       EncodingManager     _encodingManager     = new CoreEncodingRegistry();

    public TestApplication() {
        ApplicationManager.setApplication(this, _lastDisposable);
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        var simpleName = serviceClass.getSimpleName();
        if (simpleName.equals("FileDocumentManager")) {
            return (T) _fileDocumentManager;
        } else if (simpleName.equals("EncodingManager")) {
            return (T) _encodingManager;
        }
        return null;
    }

    @Override
    public void invokeLaterOnWriteThread(Runnable runnable) {

    }

    @Override
    public void invokeLaterOnWriteThread(Runnable runnable, ModalityState modalityState) {

    }

    @Override
    public void invokeLaterOnWriteThread(Runnable runnable, ModalityState modalityState, Condition<?> condition) {

    }

    @Override
    public void runReadAction(Runnable runnable) {

    }

    @Override
    public <T> T runReadAction(Computable<T> computable) {
        return null;
    }

    @Override
    public <T, E extends Throwable> T runReadAction(ThrowableComputable<T, E> throwableComputable) throws E {
        return null;
    }

    @Override
    public void runWriteAction(Runnable runnable) {

    }

    @Override
    public <T> T runWriteAction(Computable<T> computable) {
        return null;
    }

    @Override
    public <T, E extends Throwable> T runWriteAction(ThrowableComputable<T, E> throwableComputable) throws E {
        return null;
    }

    @Override
    public boolean hasWriteAction(Class<?> aClass) {
        return false;
    }

    @Override
    public void assertReadAccessAllowed() {

    }

    @Override
    public void assertWriteAccessAllowed() {

    }

    @Override
    public void assertIsDispatchThread() {

    }

    @Override
    public void assertIsWriteThread() {

    }

    @Override
    public void addApplicationListener(ApplicationListener applicationListener) {

    }

    @Override
    public void addApplicationListener(ApplicationListener applicationListener, Disposable disposable) {

    }

    @Override
    public void removeApplicationListener(ApplicationListener applicationListener) {

    }

    @Override
    public void saveAll() {

    }

    @Override
    public void saveSettings() {

    }

    @Override
    public void exit() {

    }

    @Override
    public boolean isWriteAccessAllowed() {
        return true;
    }

    @Override
    public boolean isReadAccessAllowed() {
        return true;
    }

    @Override
    public boolean isDispatchThread() {
        return false;
    }

    @Override
    public boolean isWriteThread() {
        return false;
    }

    @Override
    public ModalityInvokator getInvokator() {
        return null;
    }

    @Override
    public void invokeLater(Runnable runnable) {

    }

    @Override
    public void invokeLater(Runnable runnable, Condition<?> condition) {

    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState modalityState) {

    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState modalityState, Condition<?> condition) {

    }

    @Override
    public void invokeAndWait(Runnable runnable, ModalityState modalityState) throws ProcessCanceledException {

    }

    @Override
    public void invokeAndWait(Runnable runnable) throws ProcessCanceledException {

    }

    @Override
    public ModalityState getCurrentModalityState() {
        return null;
    }

    @Override
    public ModalityState getModalityStateForComponent(Component component) {
        return null;
    }

    @Override
    public ModalityState getDefaultModalityState() {
        return null;
    }

    @Override
    public ModalityState getNoneModalityState() {
        return null;
    }

    @Override
    public ModalityState getAnyModalityState() {
        return null;
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getIdleTime() {
        return 0;
    }

    @Override
    public boolean isUnitTestMode() {
        return false;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        return false;
    }

    @Override
    public boolean isCommandLine() {
        return false;
    }

    @Override
    public Future<?> executeOnPooledThread(Runnable runnable) {
        return null;
    }

    @Override
    public <T> Future<T> executeOnPooledThread(Callable<T> callable) {
        return null;
    }

    @Override
    public boolean isRestartCapable() {
        return false;
    }

    @Override
    public void restart() {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public AccessToken acquireReadActionLock() {
        return null;
    }

    @Override
    public AccessToken acquireWriteActionLock(Class<?> aClass) {
        return null;
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public boolean isEAP() {
        return false;
    }

    @Override
    public <T> T getComponent(Class<T> aClass) {
        return null;
    }

    @Override
    public PicoContainer getPicoContainer() {
        return null;
    }

    @Override
    public MessageBus getMessageBus() {
        return null;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public Condition<?> getDisposed() {
        return null;
    }

    @Override
    public void dispose() {

    }
}
