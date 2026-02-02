package com.tarzan.maxkb4j.module.knowledge.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

@Data
@ColumnWidth(30)
@HeadRowHeight(15)
@ContentRowHeight(20)
public class DatasetExcel {

    @ColumnWidth(30)
    @ExcelProperty(index = 0)
    private String title;
    @ColumnWidth(90)
    @ExcelProperty(index = 1)
    private String content;
    @ColumnWidth(60)
    @ExcelProperty(index = 2)
    private String problems;
}
