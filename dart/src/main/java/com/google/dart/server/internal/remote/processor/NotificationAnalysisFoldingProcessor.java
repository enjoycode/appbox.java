package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.AnalysisServerListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.dartlang.analysis.server.protocol.FoldingRegion;

public class NotificationAnalysisFoldingProcessor extends NotificationProcessor {


    public NotificationAnalysisFoldingProcessor(AnalysisServerListener listener) {
        super(listener);
    }

    @Override
    public void process(JsonObject response) throws Exception {
        JsonObject paramsObject = response.get("params").getAsJsonObject();
        String file = paramsObject.get("file").getAsString();
        JsonElement regionsElement = paramsObject.get("regions");
        getListener().computedFolding(file, FoldingRegion.fromJsonArray(regionsElement.getAsJsonArray()));
    }

}
