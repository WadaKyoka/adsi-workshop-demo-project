package com.example.attendance.leave.service;

import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.repository.LeaveBalanceRepository;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public LeaveResponse create(LeaveCreateRequest request) {
        var employee = findEmployeeOrThrow(request.requesterId());
        var leaveType = LeaveType.valueOf(request.leaveType());
        int fiscalYear = computeFiscalYear(request.leaveDate());

        var balance = leaveBalanceRepository
                .findByEmployeeIdAndFiscalYear(request.requesterId(), fiscalYear)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "有給残高が登録されていません"));

        if (balance.getRemainingDays().compareTo(leaveType.getDays()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "残日数が不足しています");
        }

        boolean duplicate = leaveRequestRepository
                .existsByRequesterIdAndLeaveDateAndStatusNot(
                        request.requesterId(), request.leaveDate(), LeaveStatus.CANCELLED);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "この日付には既に申請があります");
        }

        var leaveRequest = LeaveRequest.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .requester(employee)
                .leaveDate(request.leaveDate())
                .leaveType(leaveType)
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request created: id={}, requester={}", saved.getId(), request.requesterId());
        return LeaveResponse.from(saved);
    }

    @Override
    public List<LeaveResponse> findByRequester(UUID requesterId) {
        return leaveRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                .stream()
                .map(LeaveResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public LeaveResponse cancel(UUID leaveId, Long version) {
        var leaveRequest = findLeaveRequestOrThrow(leaveId);

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "取り下げはPENDING状態の申請のみ可能です");
        }
        validateVersion(leaveRequest, version);

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request cancelled: id={}", leaveId);
        return LeaveResponse.from(saved);
    }

    @Override
    public List<LeaveResponse> findPending(UUID managerId) {
        var manager = findEmployeeOrThrow(managerId);
        var departmentId = manager.getDepartment().getId();
        return leaveRequestRepository
                .findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                        departmentId, LeaveStatus.PENDING)
                .stream()
                .map(LeaveResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public LeaveResponse approve(UUID leaveId, UUID approverId, Long version) {
        var leaveRequest = findLeaveRequestOrThrow(leaveId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(leaveRequest, approver);
        validateVersion(leaveRequest, version);

        int fiscalYear = computeFiscalYear(leaveRequest.getLeaveDate());
        var balance = leaveBalanceRepository
                .findByEmployeeIdAndFiscalYear(leaveRequest.getRequester().getId(), fiscalYear)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "有給残高が登録されていません"));

        balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getLeaveType().getDays()));
        leaveBalanceRepository.save(balance);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprover(approver);
        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request approved: id={}, approver={}", leaveId, approverId);
        return LeaveResponse.from(saved);
    }

    @Override
    @Transactional
    public LeaveResponse reject(UUID leaveId, UUID approverId, String rejectReason, Long version) {
        var leaveRequest = findLeaveRequestOrThrow(leaveId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(leaveRequest, approver);
        validateVersion(leaveRequest, version);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprover(approver);
        leaveRequest.setRejectReason(rejectReason);
        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request rejected: id={}, approver={}", leaveId, approverId);
        return LeaveResponse.from(saved);
    }

    @Override
    public LeaveBalanceResponse getBalance(UUID employeeId) {
        var employee = findEmployeeOrThrow(employeeId);
        int fiscalYear = computeCurrentFiscalYear();
        var balance = leaveBalanceRepository
                .findByEmployeeIdAndFiscalYear(employeeId, fiscalYear)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "有給残高が登録されていません"));
        return LeaveBalanceResponse.from(balance);
    }

    @Override
    public List<LeaveBalanceResponse> getAllBalances(int fiscalYear) {
        return leaveBalanceRepository.findByFiscalYear(fiscalYear)
                .stream()
                .map(LeaveBalanceResponse::from)
                .toList();
    }

    private void validateApprover(LeaveRequest leaveRequest, Employee approver) {
        var requesterDeptId = leaveRequest.getRequester().getDepartment().getId();
        var approverDeptId = approver.getDepartment().getId();

        if (!approverDeptId.equals(requesterDeptId) || !approver.isManager()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "承認権限がありません");
        }
    }

    private void validateVersion(LeaveRequest leaveRequest, Long version) {
        if (!leaveRequest.getVersion().equals(version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "他のユーザーによって更新されました。画面を更新してください。");
        }
    }

    private LeaveRequest findLeaveRequestOrThrow(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "LeaveRequest with id '%s' was not found".formatted(id)));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee with id '%s' was not found".formatted(employeeId)));
    }

    private int computeFiscalYear(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }

    private int computeCurrentFiscalYear() {
        return computeFiscalYear(LocalDate.now());
    }
}
