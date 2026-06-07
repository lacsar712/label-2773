package com.example.employee.schedule;

import com.example.employee.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class AnnouncementScheduleTask {

    @Autowired
    private AnnouncementService announcementService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void processTimedAnnouncements() {
        announcementService.processTimedAnnouncements();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processExpiredAnnouncements() {
        announcementService.processExpiredAnnouncements();
    }
}
