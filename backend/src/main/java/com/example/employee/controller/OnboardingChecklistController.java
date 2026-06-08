package com.example.employee.controller;

import com.example.employee.annotation.AuditLog;
import com.example.employee.common.OperationType;
import com.example.employee.common.Result;
import com.example.employee.common.TargetModule;
import com.example.employee.dto.OnboardingChecklistGenerateDTO;
import com.example.employee.dto.OnboardingItemCompleteDTO;
import com.example.employee.dto.OnboardingItemDueDateDTO;
import com.example.employee.dto.OnboardingProgressDTO;
import com.example.employee.dto.OnboardingTemporaryItemDTO;
import com.example.employee.entity.OnboardingChecklist;
import com.example.employee.service.OnboardingChecklistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding/checklists")
@CrossOrigin(origins = "*")
public class OnboardingChecklistController {

    @Autowired
    private OnboardingChecklistService checklistService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<OnboardingChecklist>> getAll() {
        return Result.success(checklistService.listWithProgress());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<OnboardingChecklist> getById(@PathVariable Long id) {
        return Result.success(checklistService.getDetailWithItems(id));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<OnboardingChecklist> getByEmployeeId(@PathVariable Long employeeId) {
        return Result.success(checklistService.getByEmployeeId(employeeId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<OnboardingChecklist> getMyChecklist() {
        return Result.success(checklistService.getMyChecklist());
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<OnboardingProgressDTO> getProgress(@PathVariable Long id) {
        return Result.success(checklistService.getProgress(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_CHECKLIST, operation = OperationType.CREATE)
    public Result<OnboardingChecklist> generate(@RequestBody @Valid OnboardingChecklistGenerateDTO dto) {
        return Result.success(checklistService.generateChecklist(dto));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    @AuditLog(module = TargetModule.ONBOARDING_CHECKLIST, operation = OperationType.UPDATE)
    public Result<Boolean> completeItem(@RequestBody @Valid OnboardingItemCompleteDTO dto) {
        return Result.success(checklistService.completeItem(dto));
    }

    @PostMapping("/temporary")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_CHECKLIST, operation = OperationType.CREATE)
    public Result<Boolean> addTemporaryItem(@RequestBody @Valid OnboardingTemporaryItemDTO dto) {
        return Result.success(checklistService.addTemporaryItem(dto));
    }

    @PutMapping("/due-date")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_CHECKLIST, operation = OperationType.UPDATE)
    public Result<Boolean> updateDueDate(@RequestBody @Valid OnboardingItemDueDateDTO dto) {
        return Result.success(checklistService.updateItemDueDate(dto));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_CHECKLIST, operation = OperationType.DELETE)
    public Result<Boolean> deleteItem(@PathVariable Long itemId) {
        return Result.success(checklistService.deleteChecklistItem(itemId));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_CHECKLIST, operation = OperationType.UPDATE)
    public Result<Boolean> archive(@PathVariable Long id) {
        return Result.success(checklistService.archiveChecklist(id));
    }
}
