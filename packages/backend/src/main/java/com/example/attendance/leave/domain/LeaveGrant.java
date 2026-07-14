package com.example.attendance.leave.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record LeaveGrant(BigDecimal yearsThreshold, BigDecimal grantDays) {

    private static final List<LeaveGrant> TABLE = List.of(
            new LeaveGrant(new BigDecimal("0.5"), BigDecimal.TEN),
            new LeaveGrant(new BigDecimal("1.5"), new BigDecimal("11")),
            new LeaveGrant(new BigDecimal("2.5"), new BigDecimal("12")),
            new LeaveGrant(new BigDecimal("3.5"), new BigDecimal("14")),
            new LeaveGrant(new BigDecimal("4.5"), new BigDecimal("16")),
            new LeaveGrant(new BigDecimal("5.5"), new BigDecimal("18")),
            new LeaveGrant(new BigDecimal("6.5"), new BigDecimal("20"))
    );

    public static BigDecimal calculate(LocalDate hireDate, LocalDate referenceDate) {
        long totalDays = ChronoUnit.DAYS.between(hireDate, referenceDate);
        BigDecimal years = new BigDecimal(totalDays).divide(new BigDecimal("365.25"), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal result = BigDecimal.ZERO;
        for (LeaveGrant grant : TABLE) {
            if (years.compareTo(grant.yearsThreshold()) >= 0) {
                result = grant.grantDays();
            } else {
                break;
            }
        }
        return result;
    }
}
