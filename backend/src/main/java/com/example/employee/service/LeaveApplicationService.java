package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.LeaveApplicationQueryDTO;
import com.example.employee.dto.LeaveApprovalDTO;
import com.example.employee.dto.LeaveDaysCalculateDTO;
import com.example.employee.entity.*;
import com.example.employee.mapper.LeaveApplicationMapper;
import com.example.employee.mapper.LeaveApprovalNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveApplicationService extends ServiceImpl<LeaveApplicationMapper, LeaveApplication> {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveApprovalConfigService leaveApprovalConfigService;

    @Autowired
    private LeaveApprovalNodeMapper approvalNodeMapper;

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    private static final String[] LEAVE_TYPE_NAMES = {"", "年假", "事假", "病假", "调休"};
    private static final String[] STATUS_NAMES = {"待提交", "审批中", "已通过", "已驳回", "已撤销"};

    public LeaveDaysCalculateDTO calculateWorkDays(LeaveDaysCalculateDTO dto) {
        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate();

        if (start.isAfter(end)) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }

        long workDays = holidayService.countWorkDays(start, end);
        BigDecimal totalDays = BigDecimal.valueOf(workDays);

        Integer startHalf = dto.getStartHalf() != null ? dto.getStartHalf() : 0;
        Integer endHalf = dto.getEndHalf() != null ? dto.getEndHalf() : 0;

        if (start.equals(end)) {
            if (startHalf != 0 || endHalf != 0) {
                totalDays = BigDecimal.valueOf(0.5);
            }
        } else {
            if (startHalf != 0 && holidayService.isWorkDay(start)) {
                totalDays = totalDays.subtract(BigDecimal.valueOf(0.5));
            }
            if (endHalf != 0 && holidayService.isWorkDay(end)) {
                totalDays = totalDays.subtract(BigDecimal.valueOf(0.5));
            }
        }

        if (totalDays.compareTo(BigDecimal.ZERO) < 0) {
            totalDays = BigDecimal.ZERO;
        }

        dto.setWorkDays(totalDays.setScale(2, RoundingMode.HALF_UP));
        return dto;
    }

    @Transactional
    public LeaveApplication createDraft(LeaveApplication application) {
        Employee employee = employeeService.getById(application.getEmployeeId());
        if (employee == null) {
            throw new RuntimeException("员工不存在");
        }

        Department dept = departmentService.getById(employee.getDepartmentId());

        application.setApplicationNo(generateApplicationNo());
        application.setEmployeeName(employee.getName());
        application.setDepartmentId(employee.getDepartmentId());
        application.setDepartmentName(dept != null ? dept.getName() : null);
        application.setStatus(0);
        application.setCurrentNodeIndex(0);

        if (application.getStartHalf() == null) application.setStartHalf(0);
        if (application.getEndHalf() == null) application.setEndHalf(0);

        LeaveDaysCalculateDTO calcDto = new LeaveDaysCalculateDTO();
        calcDto.setStartDate(application.getStartDate());
        calcDto.setEndDate(application.getEndDate());
        calcDto.setStartHalf(application.getStartHalf());
        calcDto.setEndHalf(application.getEndHalf());
        calcDto = calculateWorkDays(calcDto);
        application.setTotalDays(calcDto.getWorkDays());

        save(application);
        return application;
    }

    @Transactional
    public LeaveApplication submitApplication(Long applicationId) {
        LeaveApplication application = getById(applicationId);
        if (application == null) {
            throw new RuntimeException("请假申请不存在");
        }
        if (application.getStatus() != 0) {
            throw new RuntimeException("只有待提交的申请可以提交");
        }

        if (application.getLeaveType() == 1 || application.getLeaveType() == 4) {
            if (!leaveBalanceService.checkBalance(
                    application.getEmployeeId(),
                    application.getLeaveType(),
                    application.getStartDate().getYear(),
                    application.getTotalDays())) {
                throw new RuntimeException("假期余额不足");
            }
        }

        List<LeaveApprovalConfig> configs = leaveApprovalConfigService.getConfigsForApplication(
                application.getLeaveType(),
                application.getDepartmentId(),
                application.getTotalDays()
        );

        if (configs.isEmpty()) {
            throw new RuntimeException("未找到审批流程配置");
        }

        List<LeaveApprovalNode> nodes = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            LeaveApprovalConfig config = configs.get(i);
            Employee approver = leaveApprovalConfigService.resolveApprover(
                    config, application.getEmployeeId(), application.getDepartmentId());

            if (approver == null) {
                continue;
            }

            if (approver.getId().equals(application.getEmployeeId()) && i < configs.size() - 1) {
                continue;
            }

            LeaveApprovalNode node = new LeaveApprovalNode();
            node.setApplicationId(application.getId());
            node.setNodeIndex(i);
            node.setNodeName(config.getNodeName());
            node.setApproverId(approver.getId());
            node.setApproverName(approver.getName());
            node.setNodeType(1);
            node.setStatus(i == 0 ? 0 : 0);
            nodes.add(node);
        }

        if (nodes.isEmpty()) {
            throw new RuntimeException("未找到有效审批人");
        }

        for (LeaveApprovalNode node : nodes) {
            approvalNodeMapper.insert(node);
        }

        LeaveApprovalNode firstNode = nodes.get(0);
        application.setStatus(1);
        application.setCurrentNodeIndex(0);
        application.setCurrentApproverId(firstNode.getApproverId());
        application.setCurrentApproverName(firstNode.getApproverName());
        application.setSubmitTime(LocalDateTime.now());
        updateById(application);

        return getApplicationDetail(applicationId);
    }

    @Transactional
    public LeaveApplication approveApplication(LeaveApprovalDTO dto) {
        LeaveApplication application = getById(dto.getApplicationId());
        if (application == null) {
            throw new RuntimeException("请假申请不存在");
        }
        if (application.getStatus() != 1) {
            throw new RuntimeException("只有审批中的申请可以审批");
        }

        boolean isAdminOrHr = "ADMIN".equals(dto.getApproverRoleCode()) || "HR".equals(dto.getApproverRoleCode());

        LambdaQueryWrapper<LeaveApprovalNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(LeaveApprovalNode::getApplicationId, dto.getApplicationId())
                .eq(LeaveApprovalNode::getNodeIndex, application.getCurrentNodeIndex())
                .orderByAsc(LeaveApprovalNode::getId);
        List<LeaveApprovalNode> currentNodes = approvalNodeMapper.selectList(nodeWrapper);

        LeaveApprovalNode currentNode;
        if (isAdminOrHr) {
            currentNode = currentNodes.stream()
                    .filter(n -> n.getStatus() == 0)
                    .findFirst()
                    .orElse(null);
        } else {
            currentNode = currentNodes.stream()
                    .filter(n -> n.getStatus() == 0 && (dto.getApproverId() == null
                            || n.getApproverId().equals(dto.getApproverId())))
                    .findFirst()
                    .orElse(null);

            if (currentNode == null && dto.getApproverId() != null) {
                currentNode = currentNodes.stream()
                        .filter(n -> n.getStatus() == 0)
                        .findFirst()
                        .orElse(null);
            }
        }

        if (currentNode == null) {
            currentNode = currentNodes.isEmpty() ? null : currentNodes.get(0);
        }

        if (currentNode == null) {
            throw new RuntimeException("未找到当前审批节点");
        }

        if (dto.getTransferToApproverId() != null) {
            return transferApproval(dto, application, currentNode);
        }

        if (dto.getAddSignApproverId() != null) {
            return addSignApproval(dto, application, currentNode);
        }

        currentNode.setStatus(dto.getStatus() == 1 ? 1 : 2);
        currentNode.setApprovalRemark(dto.getApprovalRemark());
        currentNode.setApprovalTime(LocalDateTime.now());
        approvalNodeMapper.updateById(currentNode);

        if (dto.getStatus() == 2) {
            application.setStatus(3);
            application.setApprovalTime(LocalDateTime.now());
            application.setCurrentApproverId(null);
            application.setCurrentApproverName(null);
            updateById(application);
            return getApplicationDetail(dto.getApplicationId());
        }

        LambdaQueryWrapper<LeaveApprovalNode> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(LeaveApprovalNode::getApplicationId, dto.getApplicationId())
                .orderByAsc(LeaveApprovalNode::getNodeIndex);
        List<LeaveApprovalNode> allNodes = approvalNodeMapper.selectList(allWrapper);

        int maxNodeIndex = allNodes.stream()
                .mapToInt(LeaveApprovalNode::getNodeIndex)
                .max()
                .orElse(0);

        boolean allCurrentApproved = allNodes.stream()
                .filter(n -> n.getNodeIndex().equals(application.getCurrentNodeIndex()))
                .allMatch(n -> n.getStatus() == 1);

        if (allCurrentApproved && application.getCurrentNodeIndex() >= maxNodeIndex) {
            application.setStatus(2);
            application.setApprovalTime(LocalDateTime.now());
            application.setCurrentApproverId(null);
            application.setCurrentApproverName(null);
            updateById(application);

            onApplicationApproved(application);
        } else if (allCurrentApproved) {
            int nextIndex = application.getCurrentNodeIndex() + 1;
            List<LeaveApprovalNode> nextNodes = allNodes.stream()
                    .filter(n -> n.getNodeIndex().equals(nextIndex))
                    .toList();

            if (!nextNodes.isEmpty()) {
                LeaveApprovalNode next = nextNodes.get(0);
                application.setCurrentNodeIndex(nextIndex);
                application.setCurrentApproverId(next.getApproverId());
                application.setCurrentApproverName(next.getApproverName());
                updateById(application);
            }
        }

        return getApplicationDetail(dto.getApplicationId());
    }

    private LeaveApplication transferApproval(LeaveApprovalDTO dto, LeaveApplication application, LeaveApprovalNode node) {
        Employee newApprover = employeeService.getById(dto.getTransferToApproverId());
        if (newApprover == null) {
            throw new RuntimeException("转交的审批人不存在");
        }

        node.setStatus(3);
        node.setApprovalRemark(dto.getApprovalRemark());
        node.setApprovalTime(LocalDateTime.now());
        node.setOriginalApproverId(node.getApproverId());
        node.setOriginalApproverName(node.getApproverName());
        approvalNodeMapper.updateById(node);

        LeaveApprovalNode newNode = new LeaveApprovalNode();
        newNode.setApplicationId(application.getId());
        newNode.setNodeIndex(node.getNodeIndex());
        newNode.setNodeName(node.getNodeName());
        newNode.setApproverId(newApprover.getId());
        newNode.setApproverName(newApprover.getName());
        newNode.setNodeType(1);
        newNode.setStatus(0);
        approvalNodeMapper.insert(newNode);

        application.setCurrentApproverId(newApprover.getId());
        application.setCurrentApproverName(newApprover.getName());
        updateById(application);

        return getApplicationDetail(dto.getApplicationId());
    }

    private LeaveApplication addSignApproval(LeaveApprovalDTO dto, LeaveApplication application, LeaveApprovalNode node) {
        Employee addSignApprover = employeeService.getById(dto.getAddSignApproverId());
        if (addSignApprover == null) {
            throw new RuntimeException("加签审批人不存在");
        }

        LeaveApprovalNode addSignNode = new LeaveApprovalNode();
        addSignNode.setApplicationId(application.getId());
        addSignNode.setNodeIndex(node.getNodeIndex());
        addSignNode.setNodeName(node.getNodeName() + "(加签)");
        addSignNode.setApproverId(addSignApprover.getId());
        addSignNode.setApproverName(addSignApprover.getName());
        addSignNode.setNodeType(2);
        addSignNode.setStatus(0);
        addSignNode.setAddSignApproverId(node.getApproverId());
        addSignNode.setAddSignApproverName(node.getApproverName());
        approvalNodeMapper.insert(addSignNode);

        application.setCurrentApproverId(addSignApprover.getId());
        application.setCurrentApproverName(addSignApprover.getName());
        updateById(application);

        return getApplicationDetail(dto.getApplicationId());
    }

    private void onApplicationApproved(LeaveApplication application) {
        if (application.getLeaveType() == 1 || application.getLeaveType() == 4) {
            leaveBalanceService.deductBalance(
                    application.getEmployeeId(),
                    application.getLeaveType(),
                    application.getStartDate().getYear(),
                    application.getTotalDays()
            );
        }

        syncToAttendance(application);
    }

    private void syncToAttendance(LeaveApplication application) {
        LocalDate start = application.getStartDate();
        LocalDate end = application.getEndDate();
        LocalDate current = start;

        while (!current.isAfter(end)) {
            if (holidayService.isWorkDay(current)) {
                AttendanceRecord record = attendanceRecordService.getEmployeeRecordForDate(
                        application.getEmployeeId(), current);
                if (record == null) {
                    record = new AttendanceRecord();
                    record.setEmployeeId(application.getEmployeeId());
                    record.setEmployeeName(application.getEmployeeName());
                    record.setDepartmentId(application.getDepartmentId());
                    record.setAttendanceDate(current);
                    record.setStatus(5);
                    record.setRemark("请假：" + LEAVE_TYPE_NAMES[application.getLeaveType()]);
                    attendanceRecordService.save(record);
                } else {
                    record.setStatus(5);
                    if (record.getRemark() == null || record.getRemark().isEmpty()) {
                        record.setRemark("请假：" + LEAVE_TYPE_NAMES[application.getLeaveType()]);
                    }
                    attendanceRecordService.updateById(record);
                }
            }
            current = current.plusDays(1);
        }
    }

    @Transactional
    public LeaveApplication cancelApplication(Long applicationId) {
        LeaveApplication application = getById(applicationId);
        if (application == null) {
            throw new RuntimeException("请假申请不存在");
        }
        if (application.getStatus() == 2) {
            if (application.getLeaveType() == 1 || application.getLeaveType() == 4) {
                leaveBalanceService.refundBalance(
                        application.getEmployeeId(),
                        application.getLeaveType(),
                        application.getStartDate().getYear(),
                        application.getTotalDays()
                );
            }
        }
        if (application.getStatus() != 0 && application.getStatus() != 1 && application.getStatus() != 2) {
            throw new RuntimeException("当前状态无法撤销");
        }

        application.setStatus(4);
        application.setCurrentApproverId(null);
        application.setCurrentApproverName(null);
        updateById(application);

        return getApplicationDetail(applicationId);
    }

    public LeaveApplication getApplicationDetail(Long applicationId) {
        LeaveApplication application = getById(applicationId);
        if (application == null) {
            return null;
        }

        LambdaQueryWrapper<LeaveApprovalNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(LeaveApprovalNode::getApplicationId, applicationId)
                .orderByAsc(LeaveApprovalNode::getNodeIndex, LeaveApprovalNode::getId);
        List<LeaveApprovalNode> nodes = approvalNodeMapper.selectList(nodeWrapper);

        for (LeaveApprovalNode node : nodes) {
            node.setStatusName(getNodeStatusName(node.getStatus()));
        }

        application.setApprovalNodes(nodes);
        application.setLeaveTypeName(LEAVE_TYPE_NAMES[application.getLeaveType()]);
        application.setStatusName(STATUS_NAMES[application.getStatus()]);

        return application;
    }

    public Page<LeaveApplication> queryApplications(LeaveApplicationQueryDTO query) {
        LambdaQueryWrapper<LeaveApplication> wrapper = new LambdaQueryWrapper<>();

        if (query.getEmployeeId() != null) {
            wrapper.eq(LeaveApplication::getEmployeeId, query.getEmployeeId());
        }
        if (query.getApproverId() != null) {
            wrapper.eq(LeaveApplication::getCurrentApproverId, query.getApproverId());
        }
        if (query.getDepartmentId() != null) {
            wrapper.eq(LeaveApplication::getDepartmentId, query.getDepartmentId());
        }
        if (query.getLeaveType() != null) {
            wrapper.eq(LeaveApplication::getLeaveType, query.getLeaveType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(LeaveApplication::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(LeaveApplication::getEndDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(LeaveApplication::getStartDate, query.getEndDate());
        }
        wrapper.orderByDesc(LeaveApplication::getCreateTime);

        Page<LeaveApplication> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        for (LeaveApplication app : page.getRecords()) {
            app.setLeaveTypeName(LEAVE_TYPE_NAMES[app.getLeaveType()]);
            app.setStatusName(STATUS_NAMES[app.getStatus()]);
        }

        return page;
    }

    public Page<LeaveApplication> getMyPendingApprovals(Long approverId, boolean isAdminOrHr, Integer pageNum, Integer pageSize) {
        List<Long> applicationIds;
        if (isAdminOrHr) {
            LambdaQueryWrapper<LeaveApprovalNode> nodeWrapper = new LambdaQueryWrapper<>();
            nodeWrapper.eq(LeaveApprovalNode::getStatus, 0);
            List<LeaveApprovalNode> pendingNodes = approvalNodeMapper.selectList(nodeWrapper);
            applicationIds = pendingNodes.stream()
                    .map(LeaveApprovalNode::getApplicationId)
                    .distinct()
                    .toList();
        } else {
            if (approverId == null) {
                return new Page<>(pageNum, pageSize);
            }
            LambdaQueryWrapper<LeaveApprovalNode> nodeWrapper = new LambdaQueryWrapper<>();
            nodeWrapper.eq(LeaveApprovalNode::getApproverId, approverId)
                    .eq(LeaveApprovalNode::getStatus, 0);
            List<LeaveApprovalNode> pendingNodes = approvalNodeMapper.selectList(nodeWrapper);
            applicationIds = pendingNodes.stream()
                    .map(LeaveApprovalNode::getApplicationId)
                    .distinct()
                    .toList();
        }

        if (applicationIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        LambdaQueryWrapper<LeaveApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LeaveApplication::getId, applicationIds)
                .eq(LeaveApplication::getStatus, 1)
                .orderByDesc(LeaveApplication::getSubmitTime);

        Page<LeaveApplication> page = page(new Page<>(pageNum, pageSize), wrapper);
        for (LeaveApplication app : page.getRecords()) {
            app.setLeaveTypeName(LEAVE_TYPE_NAMES[app.getLeaveType()]);
            app.setStatusName(STATUS_NAMES[app.getStatus()]);
        }
        return page;
    }

    private String generateApplicationNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "LV" + dateStr + uuid;
    }

    private String getNodeStatusName(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            case 3 -> "已转交";
            case 4 -> "已跳过";
            default -> "-";
        };
    }
}
