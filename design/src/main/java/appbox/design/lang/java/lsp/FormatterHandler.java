package appbox.design.lang.java.lsp;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditUtil;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.JsonRpcHelpers;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public final class FormatterHandler {

    public static List<org.eclipse.lsp4j.TextEdit> format(ICompilationUnit cu, FormattingOptions options, Range range, IProgressMonitor monitor) {
        if (cu == null) {
            return Collections.emptyList();
        }
        IRegion   region   = null;
        IDocument document = null;
        try {
            document = JsonRpcHelpers.toDocument(cu.getBuffer());
            if (document != null) {
                region = (range == null ? new Region(0, document.getLength()) : getRegion(range, document));
            }
        } catch (JavaModelException e) {
            JavaLanguageServerPlugin.logException(e.getMessage(), e);
        }
        if (region == null) {
            return Collections.emptyList();
        }

        return format(cu, document, region, options, true, monitor);
    }

    private static List<org.eclipse.lsp4j.TextEdit> format(ICompilationUnit cu, IDocument document, IRegion region,
                                                           FormattingOptions options, boolean includeComments,
                                                           IProgressMonitor monitor) {
        if (cu == null || document == null || region == null || monitor.isCanceled()) {
            return Collections.emptyList();
        }

        CodeFormatter formatter = ToolFactory.createCodeFormatter(getOptions(options, cu));

        String   lineDelimiter  = TextUtilities.getDefaultLineDelimiter(document);
        String   sourceToFormat = document.get();
        int      kind           = getFormattingKind(cu, includeComments);
        TextEdit format         = formatter.format(kind, sourceToFormat, region.getOffset(), region.getLength(), 0, lineDelimiter);
        if (format == null || format.getChildren().length == 0 || monitor.isCanceled()) {
            // nothing to return
            return Collections.<org.eclipse.lsp4j.TextEdit>emptyList();
        }
        MultiTextEdit flatEdit = TextEditUtil.flatten(format);
        return convertEdits(flatEdit.getChildren(), document);
    }

    private static int getFormattingKind(ICompilationUnit cu, boolean includeComments) {
        int kind = includeComments ? CodeFormatter.F_INCLUDE_COMMENTS : 0;
        if (cu.getResource() != null && cu.getResource().getName().equals(IModule.MODULE_INFO_JAVA)) {
            kind |= CodeFormatter.K_MODULE_INFO;
        } else {
            kind |= CodeFormatter.K_COMPILATION_UNIT;
        }
        return kind;
    }

    private static IRegion getRegion(Range range, IDocument document) {
        try {
            int offset    = document.getLineOffset(range.getStart().getLine()) + range.getStart().getCharacter();
            int endOffset = document.getLineOffset(range.getEnd().getLine()) + range.getEnd().getCharacter();
            int length    = endOffset - offset;
            return new Region(offset, length);
        } catch (BadLocationException e) {
            JavaLanguageServerPlugin.logException(e.getMessage(), e);
        }
        return null;
    }

    public static Map<String, String> getOptions(FormattingOptions options, ICompilationUnit cu) {
        Map<String, String> eclipseOptions = cu.getJavaProject().getOptions(true);

        Map<String, String> customOptions = options.entrySet().stream()
                .filter(map -> chekIfValueIsNotNull(map.getValue()))
                .collect(toMap(e -> e.getKey(), e -> getOptionValue(e.getValue())));

        eclipseOptions.putAll(customOptions);

        Integer tabSize = options.getTabSize();
        if (tabSize != null) {
            int tSize = tabSize.intValue();
            if (tSize > 0) {
                eclipseOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, Integer.toString(tSize));
            }
        }
        boolean insertSpaces = options.isInsertSpaces();
        eclipseOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, insertSpaces ? JavaCore.SPACE : JavaCore.TAB);
        return eclipseOptions;
    }

    private static boolean chekIfValueIsNotNull(Either3<String, Number, Boolean> value) {
        return value.getFirst() != null || value.getSecond() != null || value.getThird() != null;
    }

    private static String getOptionValue(Either3<String, Number, Boolean> option) {
        if (option.isFirst()) {
            return option.getFirst();
        } else if (option.isSecond()) {
            return option.getSecond().toString();
        } else {
            return option.getThird().toString();
        }
    }

    private static List<org.eclipse.lsp4j.TextEdit> convertEdits(TextEdit[] edits, IDocument document) {
        return Arrays.stream(edits).map(t -> convertEdit(t, document)).collect(Collectors.toList());
    }

    private static org.eclipse.lsp4j.TextEdit convertEdit(TextEdit edit, IDocument document) {
        org.eclipse.lsp4j.TextEdit textEdit = new org.eclipse.lsp4j.TextEdit();
        if (edit instanceof ReplaceEdit) {
            ReplaceEdit replaceEdit = (ReplaceEdit) edit;
            textEdit.setNewText(replaceEdit.getText());
            int offset = edit.getOffset();
            textEdit.setRange(new Range(createPosition(document, offset), createPosition(document, offset + edit.getLength())));
        }
        return textEdit;
    }

    private static Position createPosition(IDocument document, int offset) {
        Position start = new Position();
        try {
            int lineOfOffset = document.getLineOfOffset(offset);
            start.setLine(Integer.valueOf(lineOfOffset));
            start.setCharacter(Integer.valueOf(offset - document.getLineOffset(lineOfOffset)));
        } catch (BadLocationException e) {
            JavaLanguageServerPlugin.logException(e.getMessage(), e);
        }
        return start;
    }

}
