package org.javacs.lsp;

import com.google.gson.JsonArray;

public class DocumentLink {
    public Range range;
    public String target;
    public JsonArray data;
}
