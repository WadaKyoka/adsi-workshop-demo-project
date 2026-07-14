package com.example.attendance.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveGrantTest {

    @Test
    @DisplayName("入社半年未満: 付与0日")
    void calculate_lessThanHalfYear_returnsZero() {
        var hireDate = LocalDate.of(2026, 1, 1);
        var referenceDate = LocalDate.of(2026, 4, 1);

        var result = LeaveGrant.calculate(hireDate, referenceDate);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("入社0.5年: 10日付与")
    void calculate_halfYear_returnsTen() {
        var hireDate = LocalDate.of(2025, 10, 1);
        var referenceDate = LocalDate.of(2026, 4, 1);

        var result = LeaveGrant.calculate(hireDate, referenceDate);

        assertThat(result).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    @DisplayName("入社1.5年: 11日付与")
    void calculate_oneAndHalfYears_returnsEleven() {
        var hireDate = LocalDate.of(2024, 10, 1);
        var referenceDate = LocalDate.of(2026, 4, 1);

        var result = LeaveGrant.calculate(hireDate, referenceDate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("11"));
    }

    @Test
    @DisplayName("入社2.5年: 12日付与")
    void calculate_twoAndHalfYears_returnsTwelve() {
        var hireDate = LocalDate.of(2023, 10, 1);
        var referenceDate = LocalDate.of(2026, 4, 1);

        var result = LeaveGrant.calculate(hireDate, referenceDate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("12"));
    }

    @Test
    @DisplayName("入社6.5年以上: 20日付与")
    void calculate_sixAndHalfYearsOrMore_returnsTwenty() {
        var hireDate = LocalDate.of(2018, 4, 1);
        var referenceDate = LocalDate.of(2026, 4, 1);

        var result = LeaveGrant.calculate(hireDate, referenceDate);

        assertThat(result).isEqualByComparingTo(new BigDecimal("20"));
    }
}
