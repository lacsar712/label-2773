package com.example.employee.schedule;

import com.example.employee.service.OnboardingChecklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class OnboardingOverdueCheckTask {

    @Autowired
    private OnboardingChecklistService checklistService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkOverdueItems() {
        checklistService.checkOverdueItems();
    }
}
