package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.AnnouncementCreateDTO;
import com.example.employee.dto.AnnouncementQueryDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.*;
import com.example.employee.mapper.AnnouncementMapper;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.mapper.SysRoleMapper;
import com.example.employee.mapper.SysUserMapper;
import com.example.employee.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnnouncementService extends ServiceImpl<AnnouncementMapper, Announcement> {

    @Autowired
    private AnnouncementVisibilityService visibilityService;

    @Autowired
    private AnnouncementReadService readService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private OnboardingNotificationService notificationService;

    private Employee findEmployeeByUser(SysUser user) {
        if (user == null) return null;
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            wrapper.eq(Employee::getEmail, user.getEmail());
        } else {
            String name = user.getNickname() != null ? user.getNickname() : user.getUsername();
            wrapper.eq(Employee::getName, name);
        }
        return employeeMapper.selectOne(wrapper);
    }

    private UserInfoDTO getFullUserInfo() {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user != null && (user.getEmployeeId() == null || user.getRoleId() == null || user.getNickname() == null)) {
            try {
                user = authService.getUserInfo();
                UserContext.setCurrentUser(user);
            } catch (Exception e) {
                // ignore
            }
        }
        return user;
    }

    @Transactional
    public Announcement createDraft(AnnouncementCreateDTO dto) {
        UserInfoDTO user = getFullUserInfo();
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setCoverImage(dto.getCoverImage());
        announcement.setVisibilityType(dto.getVisibilityType());
        announcement.setEffectiveTime(dto.getEffectiveTime());
        announcement.setExpireTime(dto.getExpireTime());
        announcement.setIsPinned(dto.getIsPinned() != null && dto.getIsPinned());
        announcement.setIsImportant(dto.getIsImportant() != null && dto.getIsImportant());
        announcement.setStatus(0);
        announcement.setCreatorId(user.getUserId());
        announcement.setCreatorName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        announcement.setReadCount(0);
        save(announcement);
        saveVisibilityTargets(announcement.getId(), dto.getVisibilityTargets());
        return announcement;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, AnnouncementCreateDTO dto) {
        Announcement announcement = getById(id);
        if (announcement == null) {
            return null;
        }
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setCoverImage(dto.getCoverImage());
        announcement.setVisibilityType(dto.getVisibilityType());
        announcement.setEffectiveTime(dto.getEffectiveTime());
        announcement.setExpireTime(dto.getExpireTime());
        announcement.setIsPinned(dto.getIsPinned() != null && dto.getIsPinned());
        announcement.setIsImportant(dto.getIsImportant() != null && dto.getIsImportant());
        updateById(announcement);
        visibilityService.removeByAnnouncementId(id);
        saveVisibilityTargets(id, dto.getVisibilityTargets());
        return announcement;
    }

    private void saveVisibilityTargets(Long announcementId, List<AnnouncementCreateDTO.VisibilityTargetDTO> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (AnnouncementCreateDTO.VisibilityTargetDTO target : targets) {
            AnnouncementVisibility visibility = new AnnouncementVisibility();
            visibility.setAnnouncementId(announcementId);
            visibility.setTargetType(target.getTargetType());
            visibility.setTargetId(target.getTargetId());
            visibility.setTargetName(target.getTargetName());
            visibilityService.save(visibility);
        }
    }

    @Transactional
    public boolean publishAnnouncement(Long id) {
        Announcement announcement = getById(id);
        if (announcement == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (announcement.getEffectiveTime() != null && announcement.getEffectiveTime().isAfter(now)) {
            announcement.setStatus(1);
        } else {
            announcement.setStatus(2);
            announcement.setPublishTime(now);
        }
        int targetCount = calculateTargetCount(announcement);
        announcement.setTotalTargetCount(targetCount);
        boolean result = updateById(announcement);
        if (result && announcement.getStatus() == 2 && announcement.getIsImportant()) {
            pushNotificationToTargets(announcement);
        }
        return result;
    }

    @Transactional
    public boolean togglePin(Long id, boolean pinned) {
        Announcement announcement = getById(id);
        if (announcement == null) {
            return false;
        }
        announcement.setIsPinned(pinned);
        return updateById(announcement);
    }

    @Transactional
    public boolean archiveAnnouncement(Long id) {
        Announcement announcement = getById(id);
        if (announcement == null) {
            return false;
        }
        announcement.setStatus(3);
        return updateById(announcement);
    }

    @Transactional
    public boolean deleteAnnouncement(Long id) {
        visibilityService.removeByAnnouncementId(id);
        return removeById(id);
    }

    public IPage<Announcement> pageAdmin(AnnouncementQueryDTO query) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            wrapper.like(Announcement::getTitle, query.getTitle());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Announcement::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreateTime);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public IPage<Announcement> pageUser(AnnouncementQueryDTO query, UserInfoDTO userInfo) {
        UserInfoDTO user = getFullUserInfo();
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 2);
        wrapper.and(w -> w.isNull(Announcement::getEffectiveTime).or().le(Announcement::getEffectiveTime, now));
        wrapper.and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now));
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            wrapper.like(Announcement::getTitle, query.getTitle());
        }
        wrapper.orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishTime);
        IPage<Announcement> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<Announcement> filtered = page.getRecords().stream()
                .filter(a -> isVisibleToUser(a, user))
                .peek(a -> a.setHasRead(readService.hasRead(a.getId(), user.getUserId())))
                .collect(Collectors.toList());
        page.setRecords(filtered);
        page.setTotal(filtered.size());
        return page;
    }

    private boolean isVisibleToUser(Announcement announcement, UserInfoDTO user) {
        if (announcement.getVisibilityType() == 1) {
            return true;
        }
        List<AnnouncementVisibility> visibilities = visibilityService.listByAnnouncementId(announcement.getId());
        if (announcement.getVisibilityType() == 2) {
            List<Long> deptIds = visibilities.stream()
                    .filter(v -> v.getTargetType() == 1)
                    .map(AnnouncementVisibility::getTargetId)
                    .collect(Collectors.toList());
            if (user.getEmployeeId() != null) {
                Employee emp = employeeService.getById(user.getEmployeeId());
                if (emp != null && deptIds.contains(emp.getDepartmentId())) {
                    return true;
                }
            }
        } else if (announcement.getVisibilityType() == 3) {
            List<Long> roleIds = visibilities.stream()
                    .filter(v -> v.getTargetType() == 2)
                    .map(AnnouncementVisibility::getTargetId)
                    .collect(Collectors.toList());
            if (user.getRoleId() != null && roleIds.contains(user.getRoleId())) {
                return true;
            }
        }
        return false;
    }

    public Announcement getDetail(Long id) {
        Announcement announcement = getById(id);
        if (announcement != null) {
            announcement.setVisibilityList(visibilityService.listByAnnouncementId(id));
        }
        return announcement;
    }

    public Announcement getUserDetail(Long id) {
        UserInfoDTO user = getFullUserInfo();
        Announcement announcement = getDetail(id);
        if (announcement != null) {
            announcement.setHasRead(readService.hasRead(id, user.getUserId()));
        }
        return announcement;
    }

    @Transactional
    public boolean readAnnouncement(Long id) {
        UserInfoDTO user = getFullUserInfo();
        Long employeeId = user.getEmployeeId();
        Long departmentId = null;
        if (employeeId != null) {
            Employee emp = employeeService.getById(employeeId);
            if (emp != null) {
                departmentId = emp.getDepartmentId();
            }
        }
        String userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
        boolean result = readService.markAsRead(id, user.getUserId(), userName, employeeId, departmentId);
        if (result) {
            Announcement announcement = getById(id);
            if (announcement != null) {
                announcement.setReadCount((int) readService.countReadByAnnouncementId(id));
                updateById(announcement);
            }
        }
        return result;
    }

    public List<AnnouncementRead> getReadList(Long announcementId) {
        return readService.listByAnnouncementId(announcementId);
    }

    public List<SysUser> getUnreadUsers(Long announcementId) {
        Announcement announcement = getById(announcementId);
        if (announcement == null) {
            return new ArrayList<>();
        }
        List<Long> readUserIds = readService.listByAnnouncementId(announcementId).stream()
                .map(AnnouncementRead::getUserId)
                .collect(Collectors.toList());
        List<SysUser> targetUsers = getTargetUsers(announcement);
        return targetUsers.stream()
                .filter(u -> !readUserIds.contains(u.getId()))
                .collect(Collectors.toList());
    }

    private int calculateTargetCount(Announcement announcement) {
        return getTargetUsers(announcement).size();
    }

    private List<SysUser> getTargetUsers(Announcement announcement) {
        List<SysUser> allUsers = sysUserMapper.selectList(null);
        if (announcement.getVisibilityType() == 1) {
            return allUsers;
        }
        List<AnnouncementVisibility> visibilities = visibilityService.listByAnnouncementId(announcement.getId());
        List<Employee> allEmployees = employeeService.list();
        Map<Long, Long> userToDeptMap = allEmployees.stream()
                .filter(e -> e.getId() != null && e.getDepartmentId() != null)
                .collect(Collectors.toMap(Employee::getId, Employee::getDepartmentId, (a, b) -> a));
        Map<Long, Employee> emailToEmployeeMap = allEmployees.stream()
                .filter(e -> e.getEmail() != null)
                .collect(Collectors.toMap(Employee::getEmail, e -> e, (a, b) -> a));
        Map<String, Employee> nameToEmployeeMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getName, e -> e, (a, b) -> a));
        return allUsers.stream().filter(user -> {
            if (announcement.getVisibilityType() == 2) {
                List<Long> deptIds = visibilities.stream()
                        .filter(v -> v.getTargetType() == 1)
                        .map(AnnouncementVisibility::getTargetId)
                        .collect(Collectors.toList());
                Employee emp = null;
                if (user.getEmail() != null) {
                    emp = emailToEmployeeMap.get(user.getEmail());
                }
                if (emp == null) {
                    String name = user.getNickname() != null ? user.getNickname() : user.getUsername();
                    emp = nameToEmployeeMap.get(name);
                }
                return emp != null && deptIds.contains(emp.getDepartmentId());
            } else if (announcement.getVisibilityType() == 3) {
                List<Long> roleIds = visibilities.stream()
                        .filter(v -> v.getTargetType() == 2)
                        .map(AnnouncementVisibility::getTargetId)
                        .collect(Collectors.toList());
                return roleIds.contains(user.getRoleId());
            }
            return false;
        }).collect(Collectors.toList());
    }

    private void pushNotificationToTargets(Announcement announcement) {
        List<SysUser> targetUsers = getTargetUsers(announcement);
        for (SysUser user : targetUsers) {
            OnboardingNotification notification = new OnboardingNotification();
            notification.setRecipientId(user.getId());
            notification.setRecipientName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            notification.setNotificationType(1);
            notification.setTitle("【重要公告】" + announcement.getTitle());
            notification.setContent("您有一条重要公告，请及时查看：" + announcement.getTitle());
            notification.setIsRead(false);
            notificationService.save(notification);
        }
    }

    public void processTimedAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> toPublish = lambdaQuery()
                .eq(Announcement::getStatus, 1)
                .le(Announcement::getEffectiveTime, now)
                .list();
        for (Announcement a : toPublish) {
            a.setStatus(2);
            a.setPublishTime(now);
            updateById(a);
            if (a.getIsImportant()) {
                pushNotificationToTargets(a);
            }
        }
    }

    public void processExpiredAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> toArchive = lambdaQuery()
                .eq(Announcement::getStatus, 2)
                .isNotNull(Announcement::getExpireTime)
                .lt(Announcement::getExpireTime, now)
                .list();
        for (Announcement a : toArchive) {
            a.setStatus(3);
            updateById(a);
        }
    }
}
