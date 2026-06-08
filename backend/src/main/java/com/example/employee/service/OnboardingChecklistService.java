package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.context.UserContext;
import com.example.employee.dto.OnboardingChecklistGenerateDTO;
import com.example.employee.dto.OnboardingItemCompleteDTO;
import com.example.employee.dto.OnboardingItemDueDateDTO;
import com.example.employee.dto.OnboardingProgressDTO;
import com.example.employee.dto.OnboardingTemporaryItemDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.entity.OnboardingChecklist;
import com.example.employee.entity.OnboardingChecklistItem;
import com.example.employee.entity.OnboardingNotification;
import com.example.employee.entity.OnboardingTemplate;
import com.example.employee.entity.OnboardingTemplateItem;
import com.example.employee.mapper.OnboardingChecklistItemMapper;
import com.example.employee.mapper.OnboardingChecklistMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.mapper.OnboardingNotificationMapper;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.mapper.SysUserMapper;
import com.example.employee.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OnboardingChecklistService extends ServiceImpl<OnboardingChecklistMapper, OnboardingChecklist> {

    @Autowired
    private OnboardingChecklistItemMapper checklistItemMapper;

    @Autowired
    private OnboardingNotificationMapper notificationMapper;

    @Autowired
    private OnboardingTemplateService templateService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    public List<OnboardingChecklist> listWithProgress() {
        List<OnboardingChecklist> checklists = list();
        for (OnboardingChecklist checklist : checklists) {
            recalculateAndUpdateProgress(checklist);
        }
        return checklists.stream()
                .sorted(Comparator.comparing(OnboardingChecklist::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    public OnboardingChecklist getDetailWithItems(Long id) {
        OnboardingChecklist checklist = getById(id);
        if (checklist != null) {
            enrichChecklistWithItems(checklist);
            recalculateAndUpdateProgress(checklist);
        }
        return checklist;
    }

    public OnboardingChecklist getByEmployeeId(Long employeeId) {
        LambdaQueryWrapper<OnboardingChecklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingChecklist::getEmployeeId, employeeId);
        OnboardingChecklist checklist = getOne(wrapper);
        if (checklist != null) {
            enrichChecklistWithItems(checklist);
            recalculateAndUpdateProgress(checklist);
        }
        return checklist;
    }

    public OnboardingChecklist getMyChecklist() {
        UserInfoDTO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        Long employeeId = resolveEmployeeId(currentUser);
        if (employeeId == null) {
            return null;
        }
        return getByEmployeeId(employeeId);
    }

    private Long resolveEmployeeId(UserInfoDTO currentUser) {
        if (currentUser.getEmployeeId() != null) {
            return currentUser.getEmployeeId();
        }
        Long userId = currentUser.getUserId();
        if (userId != null) {
            SysUser sysUser = sysUserMapper.selectById(userId);
            if (sysUser != null && sysUser.getEmployeeId() != null) {
                return sysUser.getEmployeeId();
            }
            if (sysUser != null) {
                LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
                boolean hasCondition = false;
                if (sysUser.getEmail() != null && !sysUser.getEmail().isEmpty()) {
                    empWrapper.eq(Employee::getEmail, sysUser.getEmail());
                    hasCondition = true;
                }
                String nameKeyword = sysUser.getNickname() != null && !sysUser.getNickname().isEmpty()
                        ? sysUser.getNickname() : sysUser.getUsername();
                if (nameKeyword != null && !nameKeyword.isEmpty()) {
                    if (hasCondition) {
                        empWrapper.or();
                    }
                    empWrapper.eq(Employee::getName, nameKeyword);
                    hasCondition = true;
                }
                if (hasCondition) {
                    Employee emp = employeeMapper.selectOne(empWrapper);
                    if (emp != null) {
                        sysUser.setEmployeeId(emp.getId());
                        sysUserMapper.updateById(sysUser);
                        return emp.getId();
                    }
                }
            }
        }
        return null;
    }

    private void enrichChecklistWithItems(OnboardingChecklist checklist) {
        LambdaQueryWrapper<OnboardingChecklistItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingChecklistItem::getChecklistId, checklist.getId())
                .orderByAsc(OnboardingChecklistItem::getStage, OnboardingChecklistItem::getSortOrder);
        List<OnboardingChecklistItem> items = checklistItemMapper.selectList(wrapper);
        items.forEach(this::checkAndUpdateOverdueStatus);
        checklist.setItems(items);
    }

    private void checkAndUpdateOverdueStatus(OnboardingChecklistItem item) {
        if (item.getStatus() != null && item.getStatus() == 2) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (item.getDueDate() != null && item.getDueDate().isBefore(today)) {
            if (item.getStatus() == null || item.getStatus() != 3) {
                item.setStatus(3);
                checklistItemMapper.updateById(item);
                if (Boolean.FALSE.equals(item.getNotificationSent())) {
                    sendOverdueNotification(item);
                    item.setNotificationSent(true);
                    checklistItemMapper.updateById(item);
                }
            }
        }
    }

    @Transactional
    public OnboardingChecklist generateChecklist(OnboardingChecklistGenerateDTO dto) {
        Employee employee = employeeService.getById(dto.getEmployeeId());
        if (employee == null) {
            throw new RuntimeException("员工不存在");
        }

        LambdaQueryWrapper<OnboardingChecklist> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(OnboardingChecklist::getEmployeeId, dto.getEmployeeId());
        if (count(existWrapper) > 0) {
            throw new RuntimeException("该员工已有入职清单");
        }

        Long templateId = dto.getTemplateId();
        if (templateId == null) {
            OnboardingTemplate matched = templateService.findMatchingTemplate(
                    employee.getDepartmentId(), employee.getRole());
            if (matched != null) {
                templateId = matched.getId();
            }
        }

        OnboardingTemplate template = null;
        if (templateId != null) {
            template = templateService.getDetailWithItems(templateId);
        }

        Department dept = null;
        if (employee.getDepartmentId() != null) {
            dept = departmentService.getById(employee.getDepartmentId());
        }

        OnboardingChecklist checklist = new OnboardingChecklist();
        checklist.setEmployeeId(employee.getId());
        checklist.setEmployeeName(employee.getName());
        checklist.setDepartmentId(employee.getDepartmentId());
        checklist.setDepartmentName(dept != null ? dept.getName() : null);
        checklist.setPosition(employee.getRole());
        checklist.setHireDate(dto.getHireDate() != null ? dto.getHireDate() : employee.getHireDate());
        checklist.setTemplateId(templateId);
        checklist.setTemplateName(template != null ? template.getTemplateName() : null);
        checklist.setMentorId(dto.getMentorId());
        checklist.setMentorName(dto.getMentorName());
        checklist.setStatus(0);
        checklist.setProgress(0);
        checklist.setPreJoinProgress(0);
        checklist.setFirstDayProgress(0);
        checklist.setFirstWeekProgress(0);
        checklist.setFirstMonthProgress(0);
        save(checklist);

        if (template != null && template.getItems() != null) {
            int sortOrder = 0;
            for (OnboardingTemplateItem templateItem : template.getItems()) {
                OnboardingChecklistItem item = new OnboardingChecklistItem();
                item.setChecklistId(checklist.getId());
                item.setEmployeeId(employee.getId());
                item.setTemplateItemId(templateItem.getId());
                item.setItemName(templateItem.getItemName());
                item.setItemDescription(templateItem.getItemDescription());
                item.setStage(templateItem.getStage());
                item.setSortOrder(sortOrder++);
                item.setIsTemporary(false);
                item.setResponsibleRole(templateItem.getResponsibleRole());
                item.setStatus(0);
                item.setNotificationSent(false);

                Long responsibleUserId = resolveResponsibleUserId(
                        templateItem.getResponsibleRole(),
                        templateItem.getResponsibleUserId(),
                        employee,
                        dto.getMentorId(),
                        dept
                );
                String responsibleUserName = resolveResponsibleUserName(
                        templateItem.getResponsibleRole(),
                        templateItem.getResponsibleUserName(),
                        employee,
                        dto.getMentorName(),
                        dept
                );
                item.setResponsibleUserId(responsibleUserId);
                item.setResponsibleUserName(responsibleUserName);

                LocalDate dueDate = checklist.getHireDate().plusDays(templateItem.getDueDays() != null ? templateItem.getDueDays() : 0);
                item.setDueDate(dueDate);

                checklistItemMapper.insert(item);

                if (responsibleUserId != null) {
                    sendAssignmentNotification(item, employee.getName());
                }
            }
        }

        enrichChecklistWithItems(checklist);
        return checklist;
    }

    private Long resolveResponsibleUserId(String role, Long specificUserId,
                                          Employee employee, Long mentorId, Department dept) {
        if ("SPECIFIC".equals(role) && specificUserId != null) {
            return specificUserId;
        }
        if ("NEW_EMPLOYEE".equals(role)) {
            return employee.getId();
        }
        if ("MENTOR".equals(role)) {
            return mentorId;
        }
        return null;
    }

    private String resolveResponsibleUserName(String role, String specificUserName,
                                              Employee employee, String mentorName, Department dept) {
        if ("SPECIFIC".equals(role) && specificUserName != null) {
            return specificUserName;
        }
        if ("NEW_EMPLOYEE".equals(role)) {
            return employee.getName();
        }
        if ("MENTOR".equals(role)) {
            return mentorName;
        }
        if ("HR".equals(role)) {
            return "HR";
        }
        if ("DEPARTMENT_HEAD".equals(role) && dept != null) {
            return dept.getLeader();
        }
        return null;
    }

    private void sendAssignmentNotification(OnboardingChecklistItem item, String employeeName) {
        OnboardingNotification notification = new OnboardingNotification();
        notification.setChecklistId(item.getChecklistId());
        notification.setChecklistItemId(item.getId());
        notification.setRecipientId(item.getResponsibleUserId());
        notification.setRecipientName(item.getResponsibleUserName());
        notification.setNotificationType(1);
        notification.setTitle("入职待办分配：" + item.getItemName());
        notification.setContent(String.format("新员工【%s】的入职待办已分配给您：%s，请在%s前完成。",
                employeeName, item.getItemName(), item.getDueDate()));
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    private void sendOverdueNotification(OnboardingChecklistItem item) {
        OnboardingChecklist checklist = getById(item.getChecklistId());
        OnboardingNotification notification = new OnboardingNotification();
        notification.setChecklistId(item.getChecklistId());
        notification.setChecklistItemId(item.getId());
        notification.setRecipientId(item.getResponsibleUserId());
        notification.setRecipientName(item.getResponsibleUserName());
        notification.setNotificationType(3);
        notification.setTitle("入职待办已逾期：" + item.getItemName());
        notification.setContent(String.format("员工【%s】的入职待办【%s】已逾期（截止日期：%s），请尽快处理！",
                checklist != null ? checklist.getEmployeeName() : "",
                item.getItemName(), item.getDueDate()));
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    @Transactional
    public boolean completeItem(OnboardingItemCompleteDTO dto) {
        OnboardingChecklistItem item = checklistItemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new RuntimeException("待办项不存在");
        }

        UserInfoDTO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未登录");
        }

        if (!hasPermissionToComplete(item, currentUser)) {
            throw new RuntimeException("您没有权限完成该待办项");
        }

        item.setStatus(2);
        item.setCompletedUserId(currentUser.getUserId());
        item.setCompletedUserName(currentUser.getNickname());
        item.setCompletedTime(LocalDateTime.now());
        item.setRemark(dto.getRemark());
        boolean result = checklistItemMapper.updateById(item) > 0;

        if (result) {
            OnboardingChecklist checklist = getById(item.getChecklistId());
            if (checklist != null) {
                enrichChecklistWithItems(checklist);
                recalculateAndUpdateProgress(checklist);
                checkAndArchiveIfCompleted(checklist);
            }
        }
        return result;
    }

    private boolean hasPermissionToComplete(OnboardingChecklistItem item, UserInfoDTO currentUser) {
        String roleCode = currentUser.getRoleCode();
        if ("ADMIN".equals(roleCode) || "HR".equals(roleCode)) {
            return true;
        }
        Long currentEmployeeId = resolveEmployeeId(currentUser);
        if (item.getResponsibleUserId() != null && currentEmployeeId != null
                && item.getResponsibleUserId().equals(currentEmployeeId)) {
            return true;
        }
        if (item.getResponsibleUserId() != null && currentUser.getUserId() != null
                && item.getResponsibleUserId().equals(currentUser.getUserId())) {
            return true;
        }
        String responsibleRole = item.getResponsibleRole();
        if ("NEW_EMPLOYEE".equals(responsibleRole) && "EMPLOYEE".equals(roleCode)) {
            if (item.getEmployeeId() != null && currentEmployeeId != null
                    && item.getEmployeeId().equals(currentEmployeeId)) {
                return true;
            }
        }
        return false;
    }

    private void recalculateAndUpdateProgress(OnboardingChecklist checklist) {
        if (checklist.getItems() == null || checklist.getItems().isEmpty()) {
            return;
        }

        List<OnboardingChecklistItem> items = checklist.getItems();
        long total = items.size();
        long completed = items.stream().filter(i -> i.getStatus() != null && i.getStatus() == 2).count();
        int overall = total == 0 ? 0 : (int) (completed * 100 / total);

        Map<Integer, List<OnboardingChecklistItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(OnboardingChecklistItem::getStage));

        int preJoin = calcStageProgress(grouped.get(1));
        int firstDay = calcStageProgress(grouped.get(2));
        int firstWeek = calcStageProgress(grouped.get(3));
        int firstMonth = calcStageProgress(grouped.get(4));

        checklist.setProgress(overall);
        checklist.setPreJoinProgress(preJoin);
        checklist.setFirstDayProgress(firstDay);
        checklist.setFirstWeekProgress(firstWeek);
        checklist.setFirstMonthProgress(firstMonth);

        updateById(checklist);
    }

    private int calcStageProgress(List<OnboardingChecklistItem> stageItems) {
        if (stageItems == null || stageItems.isEmpty()) {
            return 0;
        }
        long total = stageItems.size();
        long completed = stageItems.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == 2)
                .count();
        return (int) (completed * 100 / total);
    }

    private void checkAndArchiveIfCompleted(OnboardingChecklist checklist) {
        if (checklist.getProgress() != null && checklist.getProgress() == 100
                && (checklist.getStatus() == null || checklist.getStatus() == 0)) {
            checklist.setStatus(1);
            updateById(checklist);

            OnboardingNotification notification = new OnboardingNotification();
            notification.setChecklistId(checklist.getId());
            notification.setRecipientId(checklist.getEmployeeId());
            notification.setRecipientName(checklist.getEmployeeName());
            notification.setNotificationType(5);
            notification.setTitle("恭喜！入职流程已全部完成");
            notification.setContent(String.format("【%s】您好，您的所有入职待办已完成，入职流程已自动归档。",
                    checklist.getEmployeeName()));
            notification.setIsRead(false);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
        }
    }

    @Transactional
    public boolean addTemporaryItem(OnboardingTemporaryItemDTO dto) {
        OnboardingChecklist checklist = getById(dto.getChecklistId());
        if (checklist == null) {
            throw new RuntimeException("入职清单不存在");
        }

        LambdaQueryWrapper<OnboardingChecklistItem> maxWrapper = new LambdaQueryWrapper<>();
        maxWrapper.eq(OnboardingChecklistItem::getChecklistId, dto.getChecklistId())
                .eq(OnboardingChecklistItem::getStage, dto.getStage())
                .orderByDesc(OnboardingChecklistItem::getSortOrder)
                .last("LIMIT 1");
        OnboardingChecklistItem lastItem = checklistItemMapper.selectOne(maxWrapper);
        int nextSort = (lastItem != null && lastItem.getSortOrder() != null) ? lastItem.getSortOrder() + 1 : 0;

        OnboardingChecklistItem item = new OnboardingChecklistItem();
        item.setChecklistId(dto.getChecklistId());
        item.setEmployeeId(checklist.getEmployeeId());
        item.setItemName(dto.getItemName());
        item.setItemDescription(dto.getItemDescription());
        item.setStage(dto.getStage());
        item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : nextSort);
        item.setIsTemporary(true);
        item.setResponsibleRole(dto.getResponsibleUserId() != null ? "SPECIFIC" : "HR");
        item.setResponsibleUserId(dto.getResponsibleUserId());
        item.setResponsibleUserName(dto.getResponsibleUserName());
        item.setDueDate(dto.getDueDate());
        item.setStatus(0);
        item.setNotificationSent(false);

        boolean result = checklistItemMapper.insert(item) > 0;
        if (result && dto.getResponsibleUserId() != null) {
            sendAssignmentNotification(item, checklist.getEmployeeName());
        }
        return result;
    }

    @Transactional
    public boolean updateItemDueDate(OnboardingItemDueDateDTO dto) {
        OnboardingChecklistItem item = checklistItemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new RuntimeException("待办项不存在");
        }
        item.setDueDate(dto.getDueDate());
        if (item.getStatus() != null && item.getStatus() == 3) {
            item.setStatus(0);
            item.setNotificationSent(false);
        }
        return checklistItemMapper.updateById(item) > 0;
    }

    @Transactional
    public boolean archiveChecklist(Long id) {
        OnboardingChecklist checklist = getById(id);
        if (checklist == null) {
            throw new RuntimeException("入职清单不存在");
        }
        if (checklist.getProgress() == null || checklist.getProgress() < 100) {
            throw new RuntimeException("入职清单尚未全部完成，无法归档");
        }
        checklist.setStatus(2);
        checklist.setArchivedTime(LocalDateTime.now());
        return updateById(checklist);
    }

    @Transactional
    public boolean deleteChecklistItem(Long itemId) {
        OnboardingChecklistItem item = checklistItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("待办项不存在");
        }
        if (!Boolean.TRUE.equals(item.getIsTemporary())) {
            throw new RuntimeException("只能删除临时追加的待办项");
        }
        boolean result = checklistItemMapper.deleteById(itemId) > 0;
        if (result) {
            OnboardingChecklist checklist = getById(item.getChecklistId());
            if (checklist != null) {
                enrichChecklistWithItems(checklist);
                recalculateAndUpdateProgress(checklist);
            }
        }
        return result;
    }

    public OnboardingProgressDTO getProgress(Long checklistId) {
        OnboardingChecklist checklist = getDetailWithItems(checklistId);
        if (checklist == null) {
            return null;
        }
        OnboardingProgressDTO dto = new OnboardingProgressDTO();
        dto.setChecklistId(checklist.getId());
        dto.setEmployeeId(checklist.getEmployeeId());
        dto.setEmployeeName(checklist.getEmployeeName());
        dto.setOverallProgress(checklist.getProgress());
        dto.setPreJoinProgress(checklist.getPreJoinProgress());
        dto.setFirstDayProgress(checklist.getFirstDayProgress());
        dto.setFirstWeekProgress(checklist.getFirstWeekProgress());
        dto.setFirstMonthProgress(checklist.getFirstMonthProgress());

        List<OnboardingChecklistItem> items = checklist.getItems();
        if (items != null) {
            dto.setTotalItems(items.size());
            dto.setCompletedItems((int) items.stream().filter(i -> i.getStatus() != null && i.getStatus() == 2).count());
            dto.setPendingItems((int) items.stream().filter(i -> i.getStatus() == null || (i.getStatus() != 2 && i.getStatus() != 3)).count());
            dto.setOverdueItems((int) items.stream().filter(i -> i.getStatus() != null && i.getStatus() == 3).count());
        }
        return dto;
    }

    public void checkOverdueItems() {
        LambdaQueryWrapper<OnboardingChecklistItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(OnboardingChecklistItem::getStatus, 2)
                .lt(OnboardingChecklistItem::getDueDate, LocalDate.now())
                .and(w -> w.ne(OnboardingChecklistItem::getStatus, 3).or().eq(OnboardingChecklistItem::getNotificationSent, false));

        List<OnboardingChecklistItem> overdueItems = checklistItemMapper.selectList(wrapper);
        for (OnboardingChecklistItem item : overdueItems) {
            if (item.getStatus() == null || item.getStatus() != 3) {
                item.setStatus(3);
                checklistItemMapper.updateById(item);
            }
            if (Boolean.FALSE.equals(item.getNotificationSent())) {
                sendOverdueNotification(item);
                item.setNotificationSent(true);
                checklistItemMapper.updateById(item);
            }
        }
    }
}
