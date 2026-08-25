package com.maxkb4j.system.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.Data;

/**
 * 首页问题数排行 Excel 导出模型。
 *
 * @author tarzan
 */
@Data
@ColumnWidth(20)
@HeadRowHeight(15)
@ContentRowHeight(20)
public class QuestionRankingExcel {

    @ColumnWidth(10)
    @ExcelProperty("排名")
    private Integer rank;

    @ColumnWidth(25)
    @ExcelProperty("应用名称")
    private String name;

    @ExcelProperty("对话次数")
    private Integer chatRecordCount;

    @ExcelProperty("对话次数占比")
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.RIGHT)
    private String chatRatio;

    @ExcelProperty("活跃用户")
    private Integer chatUserCount;

    @ExcelProperty("人均对话轮次")
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.RIGHT)
    private String avgChatPerUser;
}