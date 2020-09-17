package appbox.design.services.code;

import com.sun.source.tree.Tree;

import javax.tools.Diagnostic;

public class QuickFix {

    private int line;
    private int column;
    private int endLine;
    private int endColumn;
    private int level;
    private String text;

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }

    public int getLevel() {
        return level;
    }

    public int getLevelFromKind(Diagnostic.Kind kind) {
        if(kind.equals(Diagnostic.Kind.OTHER)){
            return 0;
        }else if(kind.equals(Diagnostic.Kind.NOTE)){
            return 1;
        }else if(kind.equals(Diagnostic.Kind.WARNING)||kind.equals(Diagnostic.Kind.MANDATORY_WARNING)){
            return 2;
        }else if(kind.equals(Diagnostic.Kind.ERROR)){
            return 3;
        }
        return 0;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
