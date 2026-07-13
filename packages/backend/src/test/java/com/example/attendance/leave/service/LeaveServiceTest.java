package com.example.attendance.leave.service;

import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.repository.LeaveBalanceRepository;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private Department department;
    private Employee employee;
    private Employee manager;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(UUID.randomUUID())
                .name("開発部")
                .build();

        employee = Employee.builder()
                .id(UUID.randomUUID())
                .name("田中太郎")
                .email("tanaka@example.com")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2020, 4, 1))
                .build();

        manager = Employee.builder()
                .id(UUID.randomUUID())
                .name("鈴木課長")
                .email("suzuki@example.com")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(true)
                .hireDate(LocalDate.of(2015, 4, 1))
                .build();

        balance = LeaveBalance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .fiscalYear(2026)
                .grantedDays(new BigDecimal("20"))
                .usedDays(new BigDecimal("3"))
                .carriedDays(new BigDecimal("5"))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("申請作成")
    class Create {

        @Test
        @DisplayName("正常: 残日数あり・重複なし → PENDING で作成")
        void create_validRequest_returnsPending() {
            var request = new LeaveCreateRequest(
                    employee.getId(),
                    LocalDate.of(2026, 7, 20),
                    "FULL",
                    "家庭の事情"
            );

            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.existsByRequesterIdAndLeaveDateAndStatusNot(
                    employee.getId(), request.leaveDate(), LeaveStatus.CANCELLED))
                    .thenReturn(false);
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> {
                        LeaveRequest saved = invocation.getArgument(0);
                        saved.setVersion(0L);
                        saved.setCreatedAt(Instant.now());
                        return saved;
                    });

            var result = leaveService.create(request);

            assertThat(result.status()).isEqualTo("PENDING");
            assertThat(result.leaveType()).isEqualTo("FULL");
            assertThat(result.requesterName()).isEqualTo("田中太郎");
        }

        @Test
        @DisplayName("異常: 残日数不足 → 例外")
        void create_insufficientBalance_throwsException() {
            balance.setUsedDays(new BigDecimal("24.5"));

            var request = new LeaveCreateRequest(
                    employee.getId(),
                    LocalDate.of(2026, 7, 20),
                    "FULL",
                    null
            );

            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));

            assertThatThrownBy(() -> leaveService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("残日数が不足");
        }

        @Test
        @DisplayName("異常: 同一日に既に申請あり → 例外")
        void create_duplicateDate_throwsException() {
            var request = new LeaveCreateRequest(
                    employee.getId(),
                    LocalDate.of(2026, 7, 20),
                    "FULL",
                    null
            );

            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.existsByRequesterIdAndLeaveDateAndStatusNot(
                    employee.getId(), request.leaveDate(), LeaveStatus.CANCELLED))
                    .thenReturn(true);

            assertThatThrownBy(() -> leaveService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("既に申請");
        }
    }

    @Nested
    @DisplayName("取り下げ")
    class Cancel {

        @Test
        @DisplayName("正常: PENDING → CANCELLED")
        void cancel_pending_success() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            var result = leaveService.cancel(leaveRequest.getId(), employee.getId(), 0L);

            assertThat(result.status()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("異常: 他人の申請を取り下げ → 403")
        void cancel_otherUser_throwsForbidden() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));

            assertThatThrownBy(() -> leaveService.cancel(leaveRequest.getId(), manager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("本人");
        }

        @Test
        @DisplayName("異常: APPROVED 状態 → 例外")
        void cancel_approved_throwsException() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.APPROVED)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));

            assertThatThrownBy(() -> leaveService.cancel(leaveRequest.getId(), employee.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("承認")
    class Approve {

        @Test
        @DisplayName("正常: 上長が承認 → APPROVED + usedDays加算")
        void approve_byManager_success() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(leaveBalanceRepository.save(any(LeaveBalance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            var result = leaveService.approve(leaveRequest.getId(), manager.getId(), 0L);

            assertThat(result.status()).isEqualTo("APPROVED");
            assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("4"));
        }

        @Test
        @DisplayName("異常: 権限なし → 403")
        void approve_notManager_throwsForbidden() {
            var otherEmployee = Employee.builder()
                    .id(UUID.randomUUID())
                    .name("佐藤")
                    .department(department)
                    .isManager(false)
                    .build();

            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(otherEmployee.getId()))
                    .thenReturn(Optional.of(otherEmployee));

            assertThatThrownBy(() -> leaveService.approve(
                    leaveRequest.getId(), otherEmployee.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("却下")
    class Reject {

        @Test
        @DisplayName("正常: 上長が却下 → REJECTED")
        void reject_byManager_success() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            var result = leaveService.reject(
                    leaveRequest.getId(), manager.getId(), "日程調整をお願いします", 0L);

            assertThat(result.status()).isEqualTo("REJECTED");
        }
    }

    @Nested
    @DisplayName("残高照会")
    class Balance {

        @Test
        @DisplayName("正常: 残高情報を返す")
        void getBalance_existing_returnsBalance() {
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));

            var result = leaveService.getBalance(employee.getId());

            assertThat(result.grantedDays()).isEqualByComparingTo(new BigDecimal("20"));
            assertThat(result.remainingDays()).isEqualByComparingTo(new BigDecimal("22"));
        }
    }

    @Nested
    @DisplayName("申請一覧")
    class FindByRequester {

        @Test
        @DisplayName("正常: 申請者の一覧を返す")
        void findByRequester_returnsList() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(leaveRequestRepository.findByRequesterIdOrderByCreatedAtDesc(employee.getId()))
                    .thenReturn(List.of(leaveRequest));

            var result = leaveService.findByRequester(employee.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).leaveDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        }
    }

    @Nested
    @DisplayName("承認待ち一覧")
    class FindPending {

        @Test
        @DisplayName("正常: 部署のPENDING一覧を返す")
        void findPending_returnsDepartmentPending() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(leaveRequestRepository.findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                    department.getId(), LeaveStatus.PENDING))
                    .thenReturn(List.of(leaveRequest));

            var result = leaveService.findPending(manager.getId());

            assertThat(result).hasSize(1);
        }
    }
}
