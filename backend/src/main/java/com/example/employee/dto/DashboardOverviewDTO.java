package com.example.employee.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardOverviewDTO {
    private Long totalEmployees;
    private Long totalHeadcount;
    private Double headcountUsageRate;
    private Long newHiresThisMonth;
    private Long pendingProbation;
    private Long contractExpiringSoon;
    private Long pendingLeaveApproval;
    private Long pendingOnboarding;
    private Double totalEmployeesMoM;
    private Double newHiresMoM;
    private Double attritionRate;
    private Double attritionRateMoM;
    private List<DepartmentStatDTO> departmentStats;
    private List<TurnoverTrendDTO> turnoverTrend;
    private List<MonthlyStatDTO> monthlyHireTrend;
}
