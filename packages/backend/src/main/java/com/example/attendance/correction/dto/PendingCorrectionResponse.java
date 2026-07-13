package com.example.attendance.correction.dto;

import com.example.attendance.correction.entity.AttendanceCorrection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PendingCorrectionResponse(
    UUID id,
    UUID attendanceRecordId,
    UUID requesterId,
    String requesterName,
    LocalDate targetDate,
    Instant correctedClockIn,
    Instant correctedClockOut,
    String reason,
    String memo,
    Long version,
    Instant createdAt
) {
    public static PendingCorrectionResponse from(AttendanceCorrection correction) {
        return new PendingCorrectionResponse(
            correction.getId(),
            correction.getAttendanceRecord() != null
                ? correction.getAttendanceRecord().getId() : null,
            correction.getRequester().getId(),
            correction.getRequester().getName(),
            correction.getTargetDate(),
            correction.getCorrectedClockIn(),
            correction.getCorrectedClockOut(),
            correction.getReason(),
            correction.getMemo(),
            correction.getVersion(),
            correction.getCreatedAt()
        );
    }
}
