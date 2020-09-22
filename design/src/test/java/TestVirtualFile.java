import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TestVirtualFile extends VirtualFile {
    private String                     _name;
    private long                       _modStamp;
    private CharSequence               _content;
    private ArrayList<TestVirtualFile> _childs;
    private TestVirtualFile            _parent;

    public TestVirtualFile(String path, long modificationStamp) {
        _name     = path;
        _content  = null;
        _childs   = new ArrayList<>();
        _modStamp = modificationStamp;
    }

    public TestVirtualFile(String name, CharSequence content, long modificationStamp) {
        _name     = name;
        _content  = content;
        _modStamp = modificationStamp;
    }

    //region ====VirtualFile====
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return null;
    }

    @Override
    public String getPath() {
        return _name;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return _content == null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return _parent;
    }

    @Override
    public VirtualFile[] getChildren() {
        if (!isDirectory()) {
            return new VirtualFile[0];
        }
        return _childs.toArray(new VirtualFile[_childs.size()]);
    }

    @Override
    public OutputStream getOutputStream(Object requestor, final long newModificationStamp, long newTimeStamp) throws IOException {
        return null;
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        //final Charset charset = getCharset();
        //final String s = getContent().toString();
        //return s.getBytes(charset.name());
        final String s = _content.toString();
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public long getTimeStamp() {
        return _modStamp;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VfsUtilCore.byteStreamSkippingBOM(contentsToByteArray(), this);
    }
    //endregion


    @Override
    public FileType getFileType() {
        return JavaFileType.INSTANCE;
    }

    public void addChild(TestVirtualFile file) {
        if (!isDirectory()) {
            throw new UnsupportedOperationException();
        }
        file._parent = this;
        _childs.add(file);
    }

    @Override
    public String toString() {
        if (_parent != null) {
            return String.format("%s/%s", _parent, _name);
        }
        return _name;
    }
}
