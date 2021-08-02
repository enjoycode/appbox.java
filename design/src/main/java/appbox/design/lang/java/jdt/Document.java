package appbox.design.lang.java.jdt;

import appbox.logging.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;

import java.util.ArrayList;
import java.util.List;

public final class Document implements IBuffer { //TODO: 实现IDocument
    //TODO:暂简单实现行检测，参考LineTracker实现
    public static final class TextLine {
        public final int start;
        public final int end;     //including the line break.

        public TextLine(int start, int end) {
            this.start = start;
            this.end   = end;
        }
    }

    private       IOpenable                    owner;
    private       IFile                        file;
    private       StringBuffer                 buffer;
    private       List<IBufferChangedListener> changeListeners;
    private final Object                       lock     = new Object();
    private       boolean                      isClosed = false;

    public Document(IOpenable owner, IFile file) {
        this.owner           = owner;
        this.file            = file;
        this.changeListeners = new ArrayList<>(3);
    }

    @Override
    public boolean isClosed() {
        return this.isClosed;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void close() {
        synchronized (this.lock) {
            if (!this.isClosed) {
                this.isClosed = true;

                fireBufferChanged(new BufferChangedEvent(this, 0, 0, null));
                changeListeners.clear();
                this.buffer = null;

                Log.debug(String.format("Document[%s] closed.", file.getName()));
            }
        }
    }

    @Override
    public char getChar(int i) {
        return buffer.charAt(i);
    }

    /**
     * Returns the contents of this buffer as a character array, or <code>null</code> if
     * the buffer has not been initialized.
     * <p>
     * Callers should make no assumption about whether the returned character array
     * is or is not the genuine article or a copy. In other words, if the client
     * wishes to change this array, they should make a copy. Likewise, if the
     * client wishes to hang on to the array in its current state, they should
     * make a copy.
     * <p>
     * The returned value is undefined if the buffer is closed.
     * @return the characters contained in this buffer
     */
    @Override
    public char[] getCharacters() {
        if (buffer == null)
            return null;

        char[] dest = new char[buffer.length()];
        buffer.getChars(0, buffer.length(), dest, 0);
        return dest;
    }

    //region ====get / set Contents====

    /**
     * Returns the contents of this buffer as a <code>String</code>. Like all strings,
     * the result is an immutable value object., It can also answer <code>null</code> if
     * the buffer has not been initialized.
     * <p>
     * The returned value is undefined if the buffer is closed.
     * @return the contents of this buffer as a <code>String</code>
     */
    @Override
    public String getContents() {
        return buffer == null ? null : buffer.toString();
    }

    @Override
    public void setContents(char[] chars) {
        setContents(new String(chars));
    }

    @Override
    public void setContents(String s) {
        //CompilationUnit.openBuffer会调用此方法
        if (buffer == null)
            buffer = new StringBuffer(s);
        else
            buffer.replace(0, buffer.length(), s);
    }
    //endregion

    @Override
    public int getLength() {
        return buffer == null ? 0 : buffer.length();
    }

    @Override
    public IOpenable getOwner() {
        return owner;
    }

    @Override
    public String getText(int offset, int length) throws IndexOutOfBoundsException {
        return buffer.substring(offset, offset + length);
    }

    @Override
    public IResource getUnderlyingResource() {
        return file;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return false; //TODO:
    }

    //region ====buffer change event====
    @Override
    public void addBufferChangedListener(IBufferChangedListener listener) {
        synchronized (lock) {
            if (!changeListeners.contains(listener)) {
                changeListeners.add(listener);
            }
        }
    }

    @Override
    public void removeBufferChangedListener(IBufferChangedListener listener) {
        synchronized (lock) {
            changeListeners.remove(listener);
        }
    }

    private void fireBufferChanged(BufferChangedEvent event) {
        IBufferChangedListener[] listeners = null;
        synchronized (lock) {
            listeners = changeListeners.toArray(new IBufferChangedListener[changeListeners.size()]);
        }
        for (IBufferChangedListener listener : listeners) {
            listener.bufferChanged(event);
        }
    }
    //endregion

    //region ====append & replace====
    @Override
    public void append(char[] chars) {
        buffer.append(chars);
    }

    @Override
    public void append(String s) {
        buffer.append(s);
    }

    @Override
    public void replace(int offset, int length, char[] chars) {
        replace(offset, length, new String(chars));
    }

    @Override
    public void replace(int offset, int length, String s) {
        buffer.replace(offset, offset + length, s);
    }
    //endregion

    @Override
    public void save(IProgressMonitor monitor, boolean force) throws JavaModelException {

    }

    private List<TextLine> getLineMap() {
        var list  = new ArrayList<TextLine>(); //TODO:缓存至改变
        int pos   = 0;
        int start = 0;

        while (pos < buffer.length()) {
            var c = buffer.charAt(pos);
            if (c == '\n') {
                list.add(new TextLine(start, pos));
                start = pos + 1;
            }
            pos++;
        }
        list.add(new TextLine(start, buffer.length() - 1));

        return list;
    }

    /**
     * 将行列转换为字符位置
     */
    public int getOffset(int line, int column) {
        if (line == 0) {
            return column;
        }

        var lines = getLineMap();
        return lines.get(line - 1).end + 1 + column;
    }

    /// 根据offset改变内容
    public void changeText(int offset, int length, String newText) {
        if (newText == null || newText.isEmpty()) {
            buffer.delete(offset, offset + length);
        } else {
            buffer.replace(offset, offset + length, newText);
        }
        //需要激发变更事件,否则CompletionUnit不更新
        fireBufferChanged(new BufferChangedEvent(this, offset, length, newText));
    }

    /// 根据行列改变内容
    public void changeText(int sline, int scol, int eline, int ecol, String newText) {
        //TODO:验证及优化
        int                     spos, epos;
        List<Document.TextLine> lines = null;
        if (sline != 0 || eline != 0) {
            lines = getLineMap();
        }
        spos = sline == 0 ? scol : lines.get(sline - 1).end + 1 + scol;
        epos = eline == 0 ? ecol : lines.get(eline - 1).end + 1 + ecol;

        if (newText == null || newText.isEmpty()) {
            buffer.delete(spos, epos);
        } else {
            buffer.replace(spos, epos, newText);
        }
        //需要激发变更事件,否则CompletionUnit不更新
        fireBufferChanged(new BufferChangedEvent(this, spos, epos - spos, newText));
    }

}
