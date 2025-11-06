package com.tarzan.maxkb4j.module.model.custom.model.disabled;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ModelDisabledException;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.List;

public class DisabledScoringModel implements ScoringModel {
    @Override
    public Response<Double> score(String text, String query) {
        throw new ModelDisabledException("ScoringModel is disabled");
    }

    @Override
    public Response<Double> score(TextSegment segment, String query) {
        throw new ModelDisabledException("ScoringModel is disabled");
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> list, String s) {
        throw new ModelDisabledException("ScoringModel is disabled");
    }
}
