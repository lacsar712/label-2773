package com.example.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.employee.common.Result;
import com.example.employee.dto.AnnouncementCreateDTO;
import com.example.employee.dto.AnnouncementQueryDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.Announcement;
import com.example.employee.entity.AnnouncementRead;
import com.example.employee.entity.SysUser;
import com.example.employee.service.AnnouncementService;
import com.example.employee.context.UserContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Announcement> createDraft(@Valid @RequestBody AnnouncementCreateDTO dto) {
        Announcement announcement = announcementService.createDraft(dto);
        return Result.success(announcement);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Announcement> update(@PathVariable Long id, @Valid @RequestBody AnnouncementCreateDTO dto) {
        Announcement announcement = announcementService.updateAnnouncement(id, dto);
        if (announcement == null) {
            return Result.error("公告不存在");
        }
        return Result.success(announcement);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Boolean> publish(@PathVariable Long id) {
        boolean result = announcementService.publishAnnouncement(id);
        return Result.success(result);
    }

    @PostMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Boolean> togglePin(@PathVariable Long id, @RequestParam Boolean pinned) {
        boolean result = announcementService.togglePin(id, pinned);
        return Result.success(result);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Boolean> archive(@PathVariable Long id) {
        boolean result = announcementService.archiveAnnouncement(id);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean result = announcementService.deleteAnnouncement(id);
        return Result.success(result);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<IPage<Announcement>> pageAdmin(AnnouncementQueryDTO query) {
        return Result.success(announcementService.pageAdmin(query));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Announcement> getAdminDetail(@PathVariable Long id) {
        Announcement announcement = announcementService.getDetail(id);
        if (announcement == null) {
            return Result.error("公告不存在");
        }
        return Result.success(announcement);
    }

    @GetMapping("/admin/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Map<String, Object>> getReadStats(@PathVariable Long id) {
        List<AnnouncementRead> readList = announcementService.getReadList(id);
        List<SysUser> unreadUsers = announcementService.getUnreadUsers(id);
        Map<String, Object> result = new HashMap<>();
        result.put("readList", readList);
        result.put("unreadList", unreadUsers);
        result.put("readCount", readList.size());
        result.put("unreadCount", unreadUsers.size());
        return Result.success(result);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<IPage<Announcement>> pageMy(AnnouncementQueryDTO query) {
        UserInfoDTO user = UserContext.getCurrentUser();
        return Result.success(announcementService.pageUser(query, user));
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Announcement> getMyDetail(@PathVariable Long id) {
        Announcement announcement = announcementService.getUserDetail(id);
        if (announcement == null) {
            return Result.error("公告不存在");
        }
        return Result.success(announcement);
    }

    @PostMapping("/my/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Boolean> markAsRead(@PathVariable Long id) {
        boolean result = announcementService.readAnnouncement(id);
        return Result.success(result);
    }
}
