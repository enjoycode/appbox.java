package appbox.design.lang.java.lsp;

import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticsHandler implements IProblemRequestor {
    private List<Diagnostic> _problems;

    public List<Diagnostic> getProblems() { return _problems; }

    @Override
    public void acceptProblem(IProblem problem) {
        var diagnostic = new Diagnostic();
        diagnostic.Text    = problem.getMessage();
        diagnostic.Line    = problem.getSourceLineNumber();
        diagnostic.EndLine = problem.getSourceLineNumber();
        if (problem instanceof DefaultProblem) {
            var dProblem = (DefaultProblem) problem;
            diagnostic.Column = dProblem.getSourceColumnNumber();
            int offset = 0;
            if (dProblem.getSourceStart() != -1 && dProblem.getSourceEnd() != -1) {
                offset = dProblem.getSourceEnd() - dProblem.getSourceStart() + 1;
            }
            diagnostic.EndColumn = dProblem.getSourceColumnNumber() + offset;
        }

        if (problem.isError()) {
            diagnostic.Level = 2;
        } else if (problem.isWarning()) {
            diagnostic.Level = 1;
        } else {
            diagnostic.Level = 0;
        }

        if (_problems == null)
            _problems = new ArrayList<>();
        _problems.add(diagnostic);
    }

    @Override
    public void beginReporting() {

    }

    @Override
    public void endReporting() {

    }

    @Override
    public boolean isActive() {
        return true;
    }

    public static class Diagnostic { //暂兼容现前端
        public int    Line;
        public int    Column;
        public int    EndLine;
        public int    EndColumn;
        public int    Level;
        public String Text;
    }
}
