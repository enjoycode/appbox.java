package org.javacs.lsp;

import com.google.gson.JsonElement;

public class RequestMessage {
    public String id, method;
    public JsonElement params;
}
