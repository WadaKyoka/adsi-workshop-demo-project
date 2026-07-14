package com.example.attendance.correction.service;

import com.example.attendance.attendance.entity.AttendanceRecord;
import com.example.attendance.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.correction.dto.CorrectionCreateRequest;
import com.example.attendance.correction.dto.CorrectionResponse;
import com.example.attendance.correction.dto.PendingCorrectionResponse;
import com.example.attendance.correction.entity.AttendanceCorrection;
import com.example.attendance.correction.entity.CorrectionStatus;
import com.example.attendance.correction.repository.AttendanceCorrectionRepository;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CorrectionServiceImpl implements CorrectionService {

    private final AttendanceCorrectionRepository correctionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;

    public CorrectionServiceImpl(
            AttendanceCorrectionRepository correctionRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            EmployeeRepository employeeRepository) {
        this.correctionRepository = correctionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public CorrectionResponse create(UUID requesterId, CorrectionCreateRequest request) {
        var requester = findEmployeeOrThrow(requesterId);

        AttendanceRecord record = null;
        if (request.attendanceRecordId() != null) {
            record = attendanceRecordRepository.findById(request.attendanceRecordId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "AttendanceRecord with id '%s' was not found"
                                    .formatted(request.attendanceRecordId())));
        }

        var correction = AttendanceCorrection.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .attendanceRecord(record)
                .requester(requester)
                .targetDate(request.targetDate())
                .correctedClockIn(request.correctedClockIn())
                .correctedClockOut(request.correctedClockOut())
                .reason(request.reason())
                .memo(request.memo())
                .status(CorrectionStatus.PENDING)
                .build();

        var saved = correctionRepository.save(correction);
        log.info("Correction created: id={}, requester={}", saved.getId(), requesterId);
        return CorrectionResponse.from(saved);
    }

    @Override
    public List<CorrectionResponse> findByRequester(UUID requesterId, CorrectionStatus status) {
        List<AttendanceCorrection> corrections;
        if (status != null) {
            corrections = correctionRepository
                    .findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId, status);
        } else {
            corrections = correctionRepository
                    .findByRequesterIdOrderByCreatedAtDesc(requesterId);
        }
        return corrections.stream()
                .map(CorrectionResponse::from)
                .toList();
    }

    @Override
    public List<PendingCorrectionResponse> findPending(UUID managerId) {
        var manager = findEmployeeOrThrow(managerId);
        var departmentId = manager.getDepartment().getId();
        var corrections = correctionRepository
                .findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                        departmentId, CorrectionStatus.PENDING);
        return corrections.stream()
                .map(PendingCorrectionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CorrectionResponse approve(UUID correctionId, UUID approverId, Long version) {
        var correction = findCorrectionOrThrow(correctionId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(correction, approver);
        validateVersion(correction, version);

        correction.setStatus(CorrectionStatus.APPROVED);
        correction.setApprover(approver);

        if (correction.getAttendanceRecord() != null) {
            var record = correction.getAttendanceRecord();
            record.setClockIn(correction.getCorrectedClockIn());
            record.setClockOut(correction.getCorrectedClockOut());
            record.setMemo(correction.getMemo());
            record.setCorrected(true);
            attendanceRecordRepository.save(record);
        } else {
            var newRecord = AttendanceRecord.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .employee(correction.getRequester())
                    .workDate(correction.getTargetDate())
                    .clockIn(correction.getCorrectedClockIn())
                    .clockOut(correction.getCorrectedClockOut())
                    .memo(correction.getMemo())
                    .corrected(true)
                    .build();
            var saved = attendanceRecordRepository.save(newRecord);
            correction.setAttendanceRecord(saved);
        }

        var saved = correctionRepository.save(correction);
        log.info("Correction approved: id={}, approver={}", correctionId, approverId);
        return CorrectionResponse.from(saved);
    }

    @Override
    @Transactional
    public CorrectionResponse reject(
            UUID correctionId, UUID approverId, String reason, Long version) {
        var correction = findCorrectionOrThrow(correctionId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(correction, approver);
        validateVersion(correction, version);

        correction.setStatus(CorrectionStatus.REJECTED);
        correction.setApprover(approver);
        correction.setRejectReason(reason);

        var saved = correctionRepository.save(correction);
        log.info("Correction rejected: id={}, approver={}", correctionId, approverId);
        return CorrectionResponse.from(saved);
    }

    private void validateApprover(AttendanceCorrection correction, Employee approver) {
        var requesterDeptId = correction.getRequester().getDepartment().getId();
        var approverDeptId = approver.getDepartment().getId();

        if (!approverDeptId.equals(requesterDeptId) || !approver.isManager()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the department manager can approve/reject corrections");
        }
    }

    private void validateVersion(AttendanceCorrection correction, Long version) {
        if (!correction.getVersion().equals(version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The correction was modified by another user. Please refresh and try again.");
        }
    }

    private AttendanceCorrection findCorrectionOrThrow(UUID correctionId) {
        return correctionRepository.findById(correctionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "AttendanceCorrection with id '%s' was not found"
                                .formatted(correctionId)));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee with id '%s' was not found".formatted(employeeId)));
    }
}
