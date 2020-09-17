package org.javacs.lsp;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.gson.JsonElement;
import java.util.List;

public class CompletionItem {
    public String label;
    public int kind;
    public String detail;
    public MarkupContent documentation;
    public Boolean deprecated, preselect;
    public String sortText, filterText, insertText;
    public Integer insertTextFormat;
    @JSONField(serialize=false)
    public TextEdit textEdit;
    @JSONField(serialize=false)
    public List<TextEdit> additionalTextEdits;
    @JSONField(serialize=false)
    public List<Character> commitCharacters;
    //@JSONField(serialize=false)
    public Command command;
    @JSONField(serialize=false)
    public JsonElement data;
}
