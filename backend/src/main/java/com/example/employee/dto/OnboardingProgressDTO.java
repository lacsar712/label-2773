package com.example.employee.dto;

import lombok.Data;

@Data
public class OnboardingProgressDTO {
    private Long checklistId;
    private Long employeeId;
    private String employeeName;
    private Integer overallProgress;
    private Integer preJoinProgress;
    private Integer firstDayProgress;
    private Integer firstWeekProgress;
    private Integer firstMonthProgress;
    private Integer totalItems;
    private Integer completedItems;
    private Integer pendingItems;
    private Integer overdueItems;
}
