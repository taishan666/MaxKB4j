package com.maxkb4j.model.custom.params;

import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.form.SingleSelectField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

@Data
public class EmbeddingModelParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        Map<String,Object> options=Map.of(
                "1,536",1536,
                "1,024",1024,
                "768",768,
                "512",512
        );
        BaseField dimension=new SingleSelectField("Dimensions",ParamKey.DIMENSIONS,"向量维度",options,1024);
        return List.of(dimension);
    }
}
