package appbox.design.services.code;

import java.util.ArrayList;
import java.util.List;

public class SourceText {
    public static final class TextLine {
        public final int start;
        public final int end;     //including the line break.

        public TextLine(int start, int end) {
            this.start = start;
            this.end   = end;
        }
    }

    private final StringBuffer sourceCode;

    public SourceText(String source) {
        this.sourceCode = new StringBuffer(source);
    }

    public List<TextLine> getLineMap() {
        var list  = new ArrayList<TextLine>(); //TODO:缓存至改变
        int pos   = 0;
        int start = 0;

        while (pos < sourceCode.length()) {
            var c = sourceCode.charAt(pos);
            if (c == '\n') {
                list.add(new TextLine(start, pos));
                start = pos + 1;
            }
            pos++;
        }
        list.add(new TextLine(start, sourceCode.length() - 1));

        return list; //TODO: toArray?
    }

    public String getLine(int line) {
        var textLine = getLineMap().get(line);
        return sourceCode.substring(textLine.start, textLine.end);
    }

    /**
     * 将行列转换为字符位置
     */
    public int getPosition(int line, int column) {
        if (line == 0) {
            return column;
        }

        var lines = getLineMap();
        return lines.get(line - 1).end + 1 + column;
    }

    public void changeText(int sline, int scol, int eline, int ecol, String newText) {
        //TODO:验证及优化
        int                       spos, epos;
        List<SourceText.TextLine> lines = null;
        if (sline != 0 || eline != 0) {
            lines = getLineMap();
        }
        spos = sline == 0 ? scol : lines.get(sline - 1).end + 1 + scol;
        epos = eline == 0 ? ecol : lines.get(eline - 1).end + 1 + ecol;

        if (newText == null) {
            sourceCode.delete(spos, epos);
        } else {
            sourceCode.replace(spos, epos, newText);
        }
    }

    @Override
    public String toString() {
        return sourceCode.toString();
    }

}
