package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveBalance;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveBalanceResponse(
        UUID employeeId,
        String employeeName,
        String departmentName,
        int fiscalYear,
        BigDecimal grantedDays,
        BigDecimal carriedDays,
        BigDecimal usedDays,
        BigDecimal remainingDays
) {
    public static LeaveBalanceResponse from(LeaveBalance balance) {
        return new LeaveBalanceResponse(
                balance.getEmployee().getId(),
                balance.getEmployee().getName(),
                balance.getEmployee().getDepartment().getName(),
                balance.getFiscalYear(),
                balance.getGrantedDays(),
                balance.getCarriedDays(),
                balance.getUsedDays(),
                balance.getRemainingDays()
        );
    }
}
