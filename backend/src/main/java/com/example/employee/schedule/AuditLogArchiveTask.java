package com.example.employee.schedule;

import com.example.employee.service.SysAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogArchiveTask {

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveOldLogs() {
        try {
            int count = sysAuditLogService.archiveLogsOlderThanOneYear();
            log.info("审计日志归档完成，归档数量：{}", count);
        } catch (Exception e) {
            log.error("审计日志归档失败", e);
        }
    }
}
