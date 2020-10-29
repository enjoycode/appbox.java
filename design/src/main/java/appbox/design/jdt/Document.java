package appbox.design.jdt;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.List;

public final class Document implements IBuffer {
    //TODO:暂简单实现行检测，参考LineTracker实现
    public static final class TextLine {
        public final int start;
        public final int end;     //including the line break.

        public TextLine(int start, int end) {
            this.start = start;
            this.end   = end;
        }
    }

    private IOpenable    owner;
    private IFile        file;
    private StringBuffer buffer;

    public Document(IOpenable owner, IFile file) {
        this.owner = owner;
        this.file  = file;
    }

    @Override
    public void append(char[] chars) {
        buffer.append(chars);
    }

    @Override
    public void append(String s) {
        buffer.append(s);
    }

    @Override
    public void close() {

    }

    @Override
    public char getChar(int i) {
        return buffer.charAt(i);
    }

    /**
     * Returns the contents of this buffer as a character array, or <code>null</code> if
     * the buffer has not been initialized.
     *
     * Callers should make no assumption about whether the returned character array
     * is or is not the genuine article or a copy. In other words, if the client
     * wishes to change this array, they should make a copy. Likewise, if the
     * client wishes to hang on to the array in its current state, they should
     * make a copy.
     *
     * The returned value is undefined if the buffer is closed.
     *
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

    /**
     * Returns the contents of this buffer as a <code>String</code>. Like all strings,
     * the result is an immutable value object., It can also answer <code>null</code> if
     * the buffer has not been initialized.
     *
     * The returned value is undefined if the buffer is closed.
     *
     * @return the contents of this buffer as a <code>String</code>
     */
    @Override
    public String getContents() {
        return buffer == null ? null : buffer.toString();
    }

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
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void addBufferChangedListener(IBufferChangedListener listener) {
    }

    @Override
    public void removeBufferChangedListener(IBufferChangedListener iBufferChangedListener) {
    }

    @Override
    public void replace(int offset, int length, char[] chars) {
        replace(offset, length, new String(chars));
    }

    @Override
    public void replace(int offset, int length, String s) {
        buffer.replace(offset, offset + length, s);
    }

    @Override
    public void save(IProgressMonitor monitor, boolean force) throws JavaModelException {

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

    public void changeText(int sline, int scol, int eline, int ecol, String newText) {
        //TODO:验证及优化
        int                     spos, epos;
        List<Document.TextLine> lines = null;
        if (sline != 0 || eline != 0) {
            lines = getLineMap();
        }
        spos = sline == 0 ? scol : lines.get(sline - 1).end + 1 + scol;
        epos = eline == 0 ? ecol : lines.get(eline - 1).end + 1 + ecol;

        if (newText == null) {
            buffer.delete(spos, epos);
        } else {
            buffer.replace(spos, epos, newText);
        }
    }

}
