package appbox.design.services.code;

public final class JavaDocument {
    public final String     fileName;
    public final SourceText sourceText;

    public JavaDocument(String fileName, String source) {
        this.fileName   = fileName;
        this.sourceText = new SourceText(source);
    }
}
