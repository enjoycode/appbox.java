package org.javacs.lsp;

public class DidOpenTextDocumentParams {
    public TextDocumentItem textDocument = new TextDocumentItem();

    public DidOpenTextDocumentParams() {}

    public DidOpenTextDocumentParams(TextDocumentItem textDocument) {
        this.textDocument = textDocument;
    }
}
