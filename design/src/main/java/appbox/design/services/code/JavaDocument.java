package appbox.design.services.code;

public final class JavaDocument {
    public final String fileName;
    public final StringBuffer sourceCode;

    public JavaDocument(String fileName, String source) {
        this.fileName = fileName;
        this.sourceCode = new StringBuffer(source);
    }
}
