package com.example.employee.controller;

import com.example.employee.common.Result;
import com.example.employee.context.UserContext;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.OnboardingNotification;
import com.example.employee.service.OnboardingNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding/notifications")
@CrossOrigin(origins = "*")
public class OnboardingNotificationController {

    @Autowired
    private OnboardingNotificationService notificationService;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<List<OnboardingNotification>> getMyNotifications() {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.success(List.of());
        }
        return Result.success(notificationService.listByRecipient(user.getUserId()));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Long> getUnreadCount() {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.success(0L);
        }
        return Result.success(notificationService.countUnreadByRecipient(user.getUserId()));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Boolean> markAsRead(@PathVariable Long id) {
        return Result.success(notificationService.markAsRead(id));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Boolean> markAllAsRead() {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.success(true);
        }
        return Result.success(notificationService.markAllAsRead(user.getUserId()));
    }
}
