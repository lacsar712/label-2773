package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.context.UserContext;
import com.example.employee.dto.DepartmentDetailDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.Department;
import com.example.employee.entity.DepartmentLeaderChangeHistory;
import com.example.employee.entity.DepartmentNotification;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.DepartmentMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.mapper.DepartmentNotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentService extends ServiceImpl<DepartmentMapper, Department> {

    @Autowired
    private DepartmentNotificationMapper notificationMapper;

    @Autowired
    private DepartmentLeaderChangeHistoryService leaderChangeHistoryService;

    @Autowired
    @Lazy
    private DepartmentVersionSnapshotService versionSnapshotService;

    @Autowired
    private EmployeeService employeeService;

    public List<Department> listWithTree() {
        List<Department> all = list();
        enrichDepartments(all);
        return buildTree(all);
    }

    public List<Department> listFlat() {
        List<Department> all = list();
        enrichDepartments(all);
        return all;
    }

    private void enrichDepartments(List<Department> departments) {
        List<Employee> allEmployees = employeeService.list();
        Map<Long, Long> deptEmpCount = allEmployees.stream()
                .filter(e -> e.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Employee::getDepartmentId, Collectors.counting()));
        Map<Long, Long> deptActiveEmpCount = allEmployees.stream()
                .filter(e -> e.getDepartmentId() != null && e.getStatus() != null && e.getStatus() != 4)
                .collect(Collectors.groupingBy(Employee::getDepartmentId, Collectors.counting()));
        Map<Long, String> deptNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        for (Department dept : departments) {
            long count = deptEmpCount.getOrDefault(dept.getId(), 0L);
            long activeCount = deptActiveEmpCount.getOrDefault(dept.getId(), 0L);
            dept.setEmployeeCount((int) count);
            dept.setActiveEmployeeCount((int) activeCount);
            dept.setOverHeadcount(dept.getHeadcountLimit() != null && activeCount > dept.getHeadcountLimit());
            if (dept.getParentId() != null) {
                dept.setParentName(deptNameMap.get(dept.getParentId()));
            }
        }
    }

    private List<Department> buildTree(List<Department> departments) {
        Map<Long, Department> map = departments.stream()
                .collect(Collectors.toMap(Department::getId, d -> d));

        List<Department> roots = new ArrayList<>();
        for (Department dept : departments) {
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                roots.add(dept);
            } else {
                Department parent = map.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                } else {
                    roots.add(dept);
                }
            }
        }
        return roots;
    }

    public boolean checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getCode, code);
        if (excludeId != null) {
            wrapper.ne(Department::getId, excludeId);
        }
        return count(wrapper) == 0;
    }

    public List<Department> searchByCode(String code) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Department::getCode, code).or().like(Department::getName, code);
        List<Department> result = list(wrapper);
        enrichDepartments(result);
        return result;
    }

    public Department getDetail(Long id) {
        Department dept = getById(id);
        if (dept != null) {
            enrichDepartments(List.of(dept));
        }
        return dept;
    }

    @Transactional
    public DepartmentDetailDTO getDepartmentFullDetail(Long id) {
        DepartmentDetailDTO dto = new DepartmentDetailDTO();
        Department dept = getDetail(id);
        dto.setDepartment(dept);

        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getDepartmentId, id)
                .in(Employee::getStatus, 1, 2, 3);
        List<Employee> activeEmployees = employeeService.list(empWrapper);
        employeeService.listWithDepartment();
        dto.setActiveEmployees(activeEmployees);

        LambdaQueryWrapper<Department> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Department::getParentId, id);
        List<Department> subDepts = list(subWrapper);
        enrichDepartments(subDepts);
        dto.setSubDepartments(subDepts);

        List<DepartmentLeaderChangeHistory> history = leaderChangeHistoryService.listByDepartmentId(id);
        dto.setLeaderChangeHistory(history);

        return dto;
    }

    @Transactional
    public boolean createDepartment(Department department) {
        if (!checkCodeUnique(department.getCode(), null)) {
            throw new RuntimeException("部门编码已存在");
        }
        boolean result = save(department);
        if (result) {
            UserInfoDTO user = UserContext.getCurrentUser();
            versionSnapshotService.createAutoSnapshot(
                    user != null ? user.getUserId() : null,
                    user != null ? user.getNickname() : null,
                    "新增部门：" + department.getName()
            );
        }
        return result;
    }

    @Transactional
    public boolean updateDepartment(Department department) {
        if (!checkCodeUnique(department.getCode(), department.getId())) {
            throw new RuntimeException("部门编码已存在");
        }
        Department oldDept = getById(department.getId());
        if (oldDept == null) {
            throw new RuntimeException("部门不存在");
        }

        boolean leaderChanged = (oldDept.getLeader() != null && !oldDept.getLeader().equals(department.getLeader()))
                || (department.getLeader() != null && !department.getLeader().equals(oldDept.getLeader()));
        boolean deputyLeaderChanged = (oldDept.getDeputyLeader() != null && !oldDept.getDeputyLeader().equals(department.getDeputyLeader()))
                || (department.getDeputyLeader() != null && !department.getDeputyLeader().equals(oldDept.getDeputyLeader()));

        boolean result = updateById(department);

        UserInfoDTO user = UserContext.getCurrentUser();
        Long operatorId = user != null ? user.getUserId() : null;
        String operatorName = user != null ? user.getNickname() : null;

        if (leaderChanged) {
            sendLeaderChangeNotification(department, oldDept);
            saveLeaderChangeHistory(department.getId(), department.getName(), 1,
                    oldDept.getLeader(), department.getLeader(), operatorId, operatorName);
        }
        if (deputyLeaderChanged) {
            saveLeaderChangeHistory(department.getId(), department.getName(), 2,
                    oldDept.getDeputyLeader(), department.getDeputyLeader(), operatorId, operatorName);
        }

        if (result) {
            StringBuilder changeDesc = new StringBuilder("更新部门：" + department.getName());
            if (leaderChanged) {
                changeDesc.append("，负责人变更");
            }
            if (deputyLeaderChanged) {
                changeDesc.append("，副负责人变更");
            }
            versionSnapshotService.createAutoSnapshot(operatorId, operatorName, changeDesc.toString());
        }
        return result;
    }

    private void saveLeaderChangeHistory(Long deptId, String deptName, Integer changeType,
                                       String oldLeader, String newLeader,
                                       Long operatorId, String operatorName) {
        DepartmentLeaderChangeHistory history = new DepartmentLeaderChangeHistory();
        history.setDepartmentId(deptId);
        history.setDepartmentName(deptName);
        history.setChangeType(changeType);
        history.setOldLeader(oldLeader);
        history.setNewLeader(newLeader);
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setCreateTime(LocalDateTime.now());
        leaderChangeHistoryService.save(history);
    }

    private void sendLeaderChangeNotification(Department newDept, Department oldDept) {
        DepartmentNotification notification = new DepartmentNotification();
        notification.setDepartmentId(newDept.getId());
        notification.setDepartmentName(newDept.getName());
        notification.setOldLeader(oldDept.getLeader());
        notification.setNewLeader(newDept.getLeader());
        notification.setContent(String.format("【%s】部门负责人已变更：由 %s 变更为 %s",
                newDept.getName(),
                oldDept.getLeader() != null ? oldDept.getLeader() : "无",
                newDept.getLeader() != null ? newDept.getLeader() : "无"));
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    @Transactional
    public boolean toggleEnabled(Long id, boolean enabled) {
        Department dept = getById(id);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        dept.setEnabled(enabled);
        updateById(dept);

        if (!enabled) {
            cascadeDisableChildren(id);
        }

        UserInfoDTO user = UserContext.getCurrentUser();
        versionSnapshotService.createAutoSnapshot(
                user != null ? user.getUserId() : null,
                user != null ? user.getNickname() : null,
                (enabled ? "启用" : "停用") + "部门：" + dept.getName()
        );
        return true;
    }

    private void cascadeDisableChildren(Long parentId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getParentId, parentId);
        List<Department> children = list(wrapper);
        for (Department child : children) {
            if (child.getEnabled()) {
                child.setEnabled(false);
                updateById(child);
            }
            cascadeDisableChildren(child.getId());
        }
    }

    @Transactional
    public boolean moveDepartment(Long deptId, Long targetParentId) {
        Department dept = getById(deptId);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        if (targetParentId != null && targetParentId.equals(deptId)) {
            throw new RuntimeException("不能将部门移动到自身下");
        }
        if (targetParentId != null) {
            List<Long> descendantIds = collectDescendantIds(deptId);
            if (descendantIds.contains(targetParentId)) {
                throw new RuntimeException("不能移动到自身的下级部门下");
            }
            Department targetParent = getById(targetParentId);
            if (targetParent == null) {
                throw new RuntimeException("目标部门不存在");
            }
        }
        Long oldParentId = dept.getParentId();
        dept.setParentId(targetParentId);
        boolean result = updateById(dept);

        if (result) {
            UserInfoDTO user = UserContext.getCurrentUser();
            versionSnapshotService.createAutoSnapshot(
                    user != null ? user.getUserId() : null,
                    user != null ? user.getNickname() : null,
                    "移动部门：" + dept.getName() + "，原上级ID：" + oldParentId + " → 新上级ID：" + targetParentId
            );
        }
        return result;
    }

    private List<Long> collectDescendantIds(Long deptId) {
        List<Long> ids = new ArrayList<>();
        collectDescendantIdsRecursive(deptId, ids);
        return ids;
    }

    private void collectDescendantIdsRecursive(Long parentId, List<Long> ids) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getParentId, parentId);
        List<Department> children = list(wrapper);
        for (Department child : children) {
            ids.add(child.getId());
            collectDescendantIdsRecursive(child.getId(), ids);
        }
    }

    @Transactional
    public boolean deleteDepartment(Long id) {
        Department dept = getById(id);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }

        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getParentId, id);
        long childCount = count(wrapper);
        if (childCount > 0) {
            throw new RuntimeException("存在下级部门，无法删除");
        }

        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getDepartmentId, id);
        long empCount = employeeService.count(empWrapper);
        if (empCount > 0) {
            throw new RuntimeException("部门下存在员工，无法删除");
        }

        boolean result = removeById(id);
        if (result) {
            UserInfoDTO user = UserContext.getCurrentUser();
            versionSnapshotService.createAutoSnapshot(
                    user != null ? user.getUserId() : null,
                    user != null ? user.getNickname() : null,
                    "删除部门：" + dept.getName()
            );
        }
        return result;
    }

    public List<Department> listEnabled() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getEnabled, true);
        return list(wrapper);
    }

    public List<DepartmentNotification> listNotifications() {
        return notificationMapper.selectList(null);
    }
}
