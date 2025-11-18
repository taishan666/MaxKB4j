package com.tarzan.maxkb4j.core.tool;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SolarTermsCalculator {

    // 节气名称顺序（从立春开始）
    private static final String[] TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    // 节气在每年中的索引偏移（因为“小寒”是第一个节气，但属于前一年1月）
    // 所以对于某年 year，节气0（小寒）实际发生在 year 年1月，而节气23（冬至）在 year 年12月
    // 因此我们按年计算时，需注意起始点

    public static void main(String[] args) {
        System.out.println(getSolarTermMonth(LocalDate.now()));
    }

    public static int getSolarTermMonth(LocalDate date) {
        int year = 2025;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < TERMS.length; i++) {
            if (i % 2 == 0) {
                LocalDate start = getSolarTermDay(date.getYear(), i);
                LocalDate end = getSolarTermDay(date.getYear(), i+1);
                if ((start.isEqual(date)||start.isBefore(date)) && end.isAfter(date)){
                    System.out.println(year + "年" + TERMS[i] + ": " + date.format(fmt));
                    return  i<2?12:i/2;
                }
            }
        }
        return 0;
    }

    public static LocalDate getSolarTermDay(int year, int term) {
        // 使用近似公式：节气日期 = floor(节气常数 + 0.2422 * (year - 基准年)) - 闰年修正
        // 数据来源：寿星万年历（适用于1900-2100年）
        // 每个节气在1月小寒开始的偏移天数（基准为1900年）
        double[] baseDays = {
                5.4055, 20.1245, 3.87, 18.73, 5.63, 20.646,
                4.81, 20.1, 5.52, 21.04, 5.675, 21.37,
                7.108, 22.83, 7.5, 23.13, 8.318, 23.042,
                8.84, 23.54, 7.438, 22.36, 7.18, 21.94
        };
        // 对应每个节气的基准年（多数为1900，部分为2000）
        int baseYear = 1900;
        // 闰年判断
        boolean isLeap = isLeapYear(year);
        // 注意：小寒、大寒属于当年1月，但冬至属于前一年12月？实际上我们统一按 year 计算所有节气
        // 但“小寒”是 year 年1月，“冬至”是 year 年12月，所以全部用 year 计算即可
        double days = baseDays[term] + 0.2422 * (year - baseYear);
        int leapCorrection = 0;
        // 闰年修正：仅对“小寒”到“大雪”之间的节气（即1月到11月）在2月后需减1？
        // 实际上标准做法：若节气在2月之后且当年是闰年，则减1（但不同资料有差异）
        // 更稳妥的方式：参考权威实现，此处采用通用简化修正：
        if (term >= 2 && isLeap) { // 从“立春”（i=2）开始，如果该年是闰年，则减1
            leapCorrection = 1;
        }
        // 特别处理 2100 年等世纪年（虽然本算法限于1900-2100，但2100不是闰年）
        // 此处暂不处理，因 isLeapYear 已正确判断
        int dayOfYear = (int) Math.floor(days) - leapCorrection;
        int month = ((term / 2) + 1);
        return LocalDate.of(year, month, LocalDate.ofEpochDay(dayOfYear).getDayOfMonth());
    }

    /**
     * 计算指定年份的所有二十四节气（精确到日）
     *
     * @param year 公历年份（如 2025）
     * @return 节气名称 -> 日期 的映射
     */
    public static LocalDate calculateSolarTerm(int year, int term) {

        // 使用近似公式：节气日期 = floor(节气常数 + 0.2422 * (year - 基准年)) - 闰年修正
        // 数据来源：寿星万年历（适用于1900-2100年）

        // 每个节气在1月小寒开始的偏移天数（基准为1900年）
        double[] baseDays = {
                5.4055, 20.1245, 3.87, 18.73, 5.63, 20.646,
                4.81, 20.1, 5.52, 21.04, 5.675, 21.37,
                7.108, 22.83, 7.5, 23.13, 8.318, 23.042,
                8.84, 23.54, 7.438, 22.36, 7.18, 21.94
        };

        // 对应每个节气的基准年（多数为1900，部分为2000）
        int baseYear = 1900;

        // 闰年判断
        boolean isLeap = isLeapYear(year);
        // 注意：小寒、大寒属于当年1月，但冬至属于前一年12月？实际上我们统一按 year 计算所有节气
        // 但“小寒”是 year 年1月，“冬至”是 year 年12月，所以全部用 year 计算即可

        double days = baseDays[term] + 0.2422 * (year - baseYear);
        int leapCorrection = 0;

        // 闰年修正：仅对“小寒”到“大雪”之间的节气（即1月到11月）在2月后需减1？
        // 实际上标准做法：若节气在2月之后且当年是闰年，则减1（但不同资料有差异）
        // 更稳妥的方式：参考权威实现，此处采用通用简化修正：
        if (term >= 2 && isLeap) { // 从“立春”（i=2）开始，如果该年是闰年，则减1
            leapCorrection = 1;
        }

        // 特别处理 2100 年等世纪年（虽然本算法限于1900-2100，但2100不是闰年）
        // 此处暂不处理，因 isLeapYear 已正确判断

        int dayOfYear = (int) Math.floor(days) - leapCorrection;
        // 将“年积日”转换为 LocalDate
        int month = ((term / 2) + 1);
        return LocalDate.of(year, month, LocalDate.ofYearDay(year, dayOfYear).getDayOfMonth());
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}