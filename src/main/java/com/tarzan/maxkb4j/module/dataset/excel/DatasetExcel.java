package com.tarzan.maxkb4j.module.dataset.excel;

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
    @ExcelProperty("分段标题（选填）")
    private String title;
    @ColumnWidth(90)
    @ExcelProperty("分段内容（必填，问题答案，最长不超过4096个字符）")
    private String content;
    @ColumnWidth(60)
    @ExcelProperty(value = "问题（选填，单元格内一行一个）")
    private String problems;
}
