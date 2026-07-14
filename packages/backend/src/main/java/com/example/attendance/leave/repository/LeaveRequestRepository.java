package com.example.attendance.leave.repository;

import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    @EntityGraph(attributePaths = {"requester"})
    List<LeaveRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    @EntityGraph(attributePaths = {"requester"})
    List<LeaveRequest> findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
            UUID departmentId, LeaveStatus status);

    boolean existsByRequesterIdAndLeaveDateAndLeaveTypeAndStatusNot(
            UUID requesterId, LocalDate leaveDate, LeaveType leaveType, LeaveStatus status);

    boolean existsByRequesterIdAndLeaveDateAndStatusNot(
            UUID requesterId, LocalDate leaveDate, LeaveStatus status);
}
