package com.tarzan.maxkb4j.module.dataset.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.tarzan.maxkb4j.module.dataset.excel.DatasetExcel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DatasetDataListener implements ReadListener<DatasetExcel> {
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
       // ReadListener.super.onException(exception, context);
        log.error(exception.getMessage(), exception);
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        ReadListener.super.invokeHead(headMap, context);
    }

    @Override
    public void invoke(DatasetExcel datasetExcel, AnalysisContext analysisContext) {

    }

    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        ReadListener.super.extra(extra, context);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        log.info("所有数据解析完成！");
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return ReadListener.super.hasNext(context);
    }
}
