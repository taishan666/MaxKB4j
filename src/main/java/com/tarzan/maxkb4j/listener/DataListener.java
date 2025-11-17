package com.tarzan.maxkb4j.listener;


import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DataListener<T> extends AnalysisEventListener<T> {
    private final List<T> dataList = new ArrayList<>();

    public DataListener() {
    }

    public void invoke(T data, AnalysisContext analysisContext) {
        this.dataList.add(data);
    }

    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }

    public void clear() {
         this.dataList.clear();
    }


}

