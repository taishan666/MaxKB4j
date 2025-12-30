package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseService {

    private final List<DocumentParser> parsers;

    public String extractText(String fileName,InputStream inputStream) {
        for (DocumentParser parser : parsers) {
            if (parser.support(fileName)) {
                return parser.handle(inputStream);
            }
        }
        return "";
    }

    public List<String> extractTable(InputStream inputStream) {
        List<String> list = new ArrayList<>();
        EasyExcel.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
            Map<Integer, String> headMap = new LinkedHashMap<>();
            // 表头信息会在此方法中获取
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                this.headMap = headMap;
            }
            // 每一行数据都会调用此方法
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
               // String sheetName = context.readSheetHolder().getSheetName();
                List<String> row = new ArrayList<>();
                for (Integer i : data.keySet()) {
                    String value = data.get(i) == null ? "" : data.get(i);
                    row.add(headMap.get(i)+":"+value);
                }
                list.add(String.join(";", row));
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                log.info("所有数据解析完成！");
            }
        }).doReadAll();
        return list;
    }

}
