package com.maxkb4j.system.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
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

    @ExcelProperty("排名")
    private Integer rank;

    @ExcelProperty("应用名称")
    private String name;

    @ExcelProperty("对话次数")
    private Integer chatRecordCount;

    @ColumnWidth(25)
    @ExcelProperty("Tokens 消耗")
    private Integer totalTokens;

    @ColumnWidth(25)
    @ExcelProperty("Tokens 占比")
    private String tokenRatio;

    @ColumnWidth(25)
    @ExcelProperty("对话次数占比")
    private String chatRatio;

    @ColumnWidth(25)
    @ExcelProperty("活跃用户")
    private Integer chatUserCount;

    @ColumnWidth(25)
    @ExcelProperty("人均对话轮次")
    private String avgChatPerUser;
}