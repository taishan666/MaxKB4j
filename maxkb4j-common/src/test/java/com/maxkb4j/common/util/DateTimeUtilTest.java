package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：日期时间工具的格式化、解析、转换与下一周期时间点计算。
 */
class DateTimeUtilTest {

    private static final LocalDateTime FIXED = LocalDateTime.of(2026, 7, 28, 10, 30, 45);

    @Test
    void format_appliesCustomPattern() {
        assertThat(DateTimeUtil.format(FIXED, "yyyy/MM/dd")).isEqualTo("2026/07/28");
        assertThat(DateTimeUtil.format(FIXED, "HH:mm:ss")).isEqualTo("10:30:45");
    }

    @Test
    void parseDateTime_defaultFormatRoundTrips() {
        assertThat(DateTimeUtil.parseDateTime("2026-07-28 10:30:45")).isEqualTo(FIXED);
    }

    @Test
    void parseDateTime_withPattern() {
        assertThat(DateTimeUtil.parseDateTime("2026/07/28 10-30-45", "yyyy/MM/dd HH-mm-ss")).isEqualTo(FIXED);
    }

    @Test
    void parseDate_defaultFormat() {
        assertThat(DateTimeUtil.parseDate("2026-07-28")).isEqualTo(LocalDate.of(2026, 7, 28));
    }

    @Test
    void parseTime_defaultFormat() {
        assertThat(DateTimeUtil.parseTime("10:30:45")).isEqualTo(LocalTime.of(10, 30, 45));
    }

    @Test
    void toInstantAndToDateTime_roundTrip() {
        Instant instant = DateTimeUtil.toInstant(FIXED);
        assertThat(DateTimeUtil.toDateTime(instant)).isEqualTo(FIXED);
    }

    @Test
    void toDateAndToLocalDate_roundTrip() {
        Date date = DateTimeUtil.toDate(FIXED);
        assertThat(DateTimeUtil.toLocalDate(date)).isEqualTo(FIXED.toLocalDate());
    }

    @Test
    void between_temporalReturnsDuration() {
        Duration duration = DateTimeUtil.between(LocalTime.of(10, 0), LocalTime.of(11, 30));
        assertThat(duration).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void between_datesReturnsPeriod() {
        Period period = DateTimeUtil.between(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
        assertThat(period.getDays()).isEqualTo(10);
    }

    @Test
    void getNextDay_returnsTomorrowAtGivenTime() {
        LocalDateTime result = DateTimeUtil.getNextDay(LocalTime.of(8, 30));
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void getNextDayAtTime_picksTodayOrTomorrowInFuture() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.getNextDayAtTime(9, 30, 0);
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(9, 30, 0));
        assertThat(result.toLocalDate()).isIn(now.toLocalDate(), now.toLocalDate().plusDays(1));
        assertThat(result).isAfter(now);
    }

    @Test
    void getSameDayNextWeek_matchesDayOfWeekAndIsInFuture() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.getSameDayNextWeek(3, 10, 0, 0);
        assertThat(result.getDayOfWeek().getValue()).isEqualTo(3);
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(10, 0, 0));
        assertThat(result).isAfter(now);
        assertThat(result.toLocalDate()).isBetween(now.toLocalDate(), now.toLocalDate().plusDays(7));
    }

    @Test
    void getSameDayNextWeek_invalidDayThrows() {
        assertThatThrownBy(() -> DateTimeUtil.getSameDayNextWeek(0, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateTimeUtil.getSameDayNextWeek(8, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSameDayNextMonth_matchesDayClampedToMonthLengthAndIsInFuture() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.getSameDayNextMonth(15, 10, 0, 0);
        assertThat(result.getDayOfMonth()).isEqualTo(Math.min(15, result.toLocalDate().lengthOfMonth()));
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(10, 0, 0));
        assertThat(result).isAfter(now);
    }

    @Test
    void getSameDayNextMonth_invalidDayThrows() {
        assertThatThrownBy(() -> DateTimeUtil.getSameDayNextMonth(0, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateTimeUtil.getSameDayNextMonth(32, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSameDayNextInterval_hoursAdvancesIntoFuture() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.getSameDayNextInterval("2", "hours", 0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result).isAfter(now);
    }

    @Test
    void getSameDayNextInterval_minutesAdvancesIntoFuture() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = DateTimeUtil.getSameDayNextInterval("30", "minutes", 0);
        assertThat(result.getSecond()).isEqualTo(0);
        assertThat(result).isAfter(now);
    }

    @Test
    void getSameDayNextInterval_invalidUnitThrows() {
        assertThatThrownBy(() -> DateTimeUtil.getSameDayNextInterval("2", "seconds", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSameDayNextInterval_invalidNumberThrows() {
        assertThatThrownBy(() -> DateTimeUtil.getSameDayNextInterval("abc", "hours", 0))
                .isInstanceOf(NumberFormatException.class);
    }
}