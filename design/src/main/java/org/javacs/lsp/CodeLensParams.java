package org.javacs.lsp;

public class CodeLensParams {
    public TextDocumentIdentifier textDocument;

    public CodeLensParams() {}

    public CodeLensParams(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }
}
