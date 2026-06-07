package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.entity.Department;
import com.example.employee.entity.DepartmentNotification;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.DepartmentMapper;
import com.example.employee.mapper.DepartmentNotificationMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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

        for (Department dept : departments) {
            long count = deptEmpCount.getOrDefault(dept.getId(), 0L);
            dept.setEmployeeCount((int) count);
            dept.setOverHeadcount(dept.getHeadcountLimit() != null && count > dept.getHeadcountLimit());
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
    public boolean createDepartment(Department department) {
        if (!checkCodeUnique(department.getCode(), null)) {
            throw new RuntimeException("部门编码已存在");
        }
        return save(department);
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

        boolean result = updateById(department);
        if (leaderChanged) {
            sendLeaderChangeNotification(department, oldDept);
        }
        return result;
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
    public boolean deleteDepartment(Long id) {
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

        return removeById(id);
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
