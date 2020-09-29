import appbox.logging.Log;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.compiled.ClsStubBuilder;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.source.JavaFileElementType;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.util.cls.ClsFormatException;
import com.intellij.util.indexing.FileContent;

public class TestClsDecompilerImpl extends ClassFileDecompilers.Full {
    private static final Logger         LOG           = Logger.getInstance(TestClsDecompilerImpl.class);
    private final        ClsStubBuilder myStubBuilder = new MyClsStubBuilder();

    @Override
    public boolean accepts(VirtualFile file) {
        return true;
    }

    @Override
    public ClsStubBuilder getStubBuilder() {
        return myStubBuilder;
    }

    @Override
    public FileViewProvider createFileViewProvider(VirtualFile file, PsiManager manager, boolean physical) {
        return new ClassFileViewProvider(manager, file, physical);
    }

    private static class MyClsStubBuilder extends ClsStubBuilder {
        @Override
        public int getStubVersion() {
            return JavaFileElementType.STUB_VERSION;
        }

        @Override
        public PsiFileStub<?> buildFileStub(FileContent fileContent) throws ClsFormatException {
            PsiFileStub<?> stub = ClsFileImpl.buildFileStub(fileContent.getFile(), fileContent.getContent());
            if (stub == null && fileContent.getFileName().indexOf('$') < 0) {
                LOG.info("No stub built for the file " + fileContent);
            }
            return stub;
        }
    }

}
