package com.example.employee.controller;

import com.example.employee.annotation.AuditLog;
import com.example.employee.common.OperationType;
import com.example.employee.common.Result;
import com.example.employee.common.TargetModule;
import com.example.employee.context.UserContext;
import com.example.employee.dto.CreateSnapshotDTO;
import com.example.employee.dto.DepartmentDetailDTO;
import com.example.employee.dto.DepartmentMoveDTO;
import com.example.employee.dto.TreeStateDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.Department;
import com.example.employee.entity.DepartmentNotification;
import com.example.employee.entity.DepartmentVersionSnapshot;
import com.example.employee.service.DepartmentService;
import com.example.employee.service.DepartmentVersionSnapshotService;
import com.example.employee.service.UserTreeStateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentVersionSnapshotService snapshotService;

    @Autowired
    private UserTreeStateService treeStateService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> getTree() {
        return Result.success(departmentService.listWithTree());
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> getList() {
        return Result.success(departmentService.listFlat());
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> getEnabled() {
        return Result.success(departmentService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Department> getById(@PathVariable Long id) {
        return Result.success(departmentService.getDetail(id));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<DepartmentDetailDTO> getFullDetail(@PathVariable Long id) {
        return Result.success(departmentService.getDepartmentFullDetail(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> search(@RequestParam String keyword) {
        return Result.success(departmentService.searchByCode(keyword));
    }

    @GetMapping("/check-code")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Boolean> checkCode(@RequestParam String code, @RequestParam(required = false) Long excludeId) {
        return Result.success(departmentService.checkCodeUnique(code, excludeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.CREATE, recordNameField = "name")
    public Result<Boolean> create(@RequestBody @Valid Department department) {
        return Result.success(departmentService.createDepartment(department));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.UPDATE, recordNameField = "name")
    public Result<Boolean> update(@RequestBody @Valid Department department) {
        return Result.success(departmentService.updateDepartment(department));
    }

    @PutMapping("/move")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.UPDATE)
    public Result<Boolean> move(@RequestBody @Valid DepartmentMoveDTO dto) {
        return Result.success(departmentService.moveDepartment(dto.getDeptId(), dto.getTargetParentId()));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.UPDATE)
    public Result<Boolean> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        return Result.success(departmentService.toggleEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(departmentService.deleteDepartment(id));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<DepartmentNotification>> getNotifications() {
        return Result.success(departmentService.listNotifications());
    }

    @GetMapping("/snapshots")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<DepartmentVersionSnapshot>> listSnapshots() {
        return Result.success(snapshotService.listAll());
    }

    @PostMapping("/snapshots")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.CREATE)
    public Result<DepartmentVersionSnapshot> createSnapshot(@RequestBody @Valid CreateSnapshotDTO dto) {
        UserInfoDTO user = UserContext.getCurrentUser();
        return Result.success(snapshotService.createSnapshot(dto,
                user != null ? user.getUserId() : null,
                user != null ? user.getNickname() : null));
    }

    @GetMapping("/snapshots/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<DepartmentVersionSnapshot> getSnapshot(@PathVariable Long id) {
        return Result.success(snapshotService.getSnapshot(id));
    }

    @GetMapping("/snapshots/{id}/preview")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> previewSnapshot(@PathVariable Long id) {
        return Result.success(snapshotService.getSnapshotTree(id));
    }

    @PostMapping("/snapshots/{id}/apply")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.UPDATE)
    public Result<Boolean> applySnapshot(@PathVariable Long id) {
        UserInfoDTO user = UserContext.getCurrentUser();
        return Result.success(snapshotService.applySnapshot(id,
                user != null ? user.getUserId() : null,
                user != null ? user.getNickname() : null));
    }

    @DeleteMapping("/snapshots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.DELETE)
    public Result<Boolean> deleteSnapshot(@PathVariable Long id) {
        return Result.success(snapshotService.deleteSnapshot(id));
    }

    @GetMapping("/tree-state")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<TreeStateDTO> getTreeState(@RequestParam(defaultValue = "department") String treeKey) {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.success(new TreeStateDTO());
        }
        return Result.success(treeStateService.getTreeState(user.getUserId(), treeKey));
    }

    @PostMapping("/tree-state")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Boolean> saveTreeState(@RequestBody TreeStateDTO dto) {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.success(false);
        }
        return Result.success(treeStateService.saveTreeState(user.getUserId(), dto));
    }
}
