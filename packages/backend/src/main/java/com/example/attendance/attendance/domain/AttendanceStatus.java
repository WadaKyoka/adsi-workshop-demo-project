package com.example.attendance.attendance.domain;

import com.example.attendance.attendance.entity.AttendanceRecord;

import java.util.List;

public enum AttendanceStatus {
    NOT_CLOCKED_IN,
    CLOCKED_IN,
    CLOCKED_OUT;

    public static AttendanceStatus fromRecords(List<AttendanceRecord> records) {
        if (records.isEmpty()) {
            return NOT_CLOCKED_IN;
        }
        if (records.stream().anyMatch(r -> r.getClockOut() == null)) {
            return CLOCKED_IN;
        }
        return CLOCKED_OUT;
    }
}
