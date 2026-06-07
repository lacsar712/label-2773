package com.example.employee.controller;

import com.example.employee.annotation.AuditLog;
import com.example.employee.common.OperationType;
import com.example.employee.common.Result;
import com.example.employee.common.TargetModule;
import com.example.employee.entity.OnboardingTemplate;
import com.example.employee.service.OnboardingTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding/templates")
@CrossOrigin(origins = "*")
public class OnboardingTemplateController {

    @Autowired
    private OnboardingTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<OnboardingTemplate>> getAll() {
        return Result.success(templateService.listWithItems());
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<OnboardingTemplate>> getEnabled() {
        return Result.success(templateService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<OnboardingTemplate> getById(@PathVariable Long id) {
        return Result.success(templateService.getDetailWithItems(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_TEMPLATE, operation = OperationType.CREATE, recordNameField = "templateName")
    public Result<Boolean> create(@RequestBody @Valid OnboardingTemplate template) {
        return Result.success(templateService.createTemplateWithItems(template));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_TEMPLATE, operation = OperationType.UPDATE, recordNameField = "templateName")
    public Result<Boolean> update(@RequestBody @Valid OnboardingTemplate template) {
        return Result.success(templateService.updateTemplateWithItems(template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.ONBOARDING_TEMPLATE, operation = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(templateService.deleteTemplateWithItems(id));
    }
}
