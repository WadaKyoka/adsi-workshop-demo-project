package com.example.attendance.correction.dto;

import com.example.attendance.correction.entity.AttendanceCorrection;
import com.example.attendance.correction.entity.CorrectionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CorrectionResponse(
    UUID id,
    UUID attendanceRecordId,
    UUID requesterId,
    String requesterName,
    UUID approverId,
    String approverName,
    LocalDate targetDate,
    Instant correctedClockIn,
    Instant correctedClockOut,
    String reason,
    String memo,
    CorrectionStatus status,
    String rejectReason,
    Long version,
    Instant createdAt
) {
    public static CorrectionResponse from(AttendanceCorrection correction) {
        return new CorrectionResponse(
            correction.getId(),
            correction.getAttendanceRecord() != null
                ? correction.getAttendanceRecord().getId() : null,
            correction.getRequester().getId(),
            correction.getRequester().getName(),
            correction.getApprover() != null
                ? correction.getApprover().getId() : null,
            correction.getApprover() != null
                ? correction.getApprover().getName() : null,
            correction.getTargetDate(),
            correction.getCorrectedClockIn(),
            correction.getCorrectedClockOut(),
            correction.getReason(),
            correction.getMemo(),
            correction.getStatus(),
            correction.getRejectReason(),
            correction.getVersion(),
            correction.getCreatedAt()
        );
    }
}
