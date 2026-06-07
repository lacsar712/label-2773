package com.example.employee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.employee.dto.AuditLogDetailDTO;
import com.example.employee.dto.AuditLogQueryDTO;
import com.example.employee.dto.FieldDiffDTO;
import com.example.employee.entity.SysAuditLog;
import com.example.employee.mapper.SysAuditLogMapper;
import com.example.employee.utils.AuditLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysAuditLogService {

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @Async
    public void saveLog(SysAuditLog log) {
        sysAuditLogMapper.insert(log);
    }

    public IPage<SysAuditLog> queryPage(AuditLogQueryDTO query) {
        Page<SysAuditLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return sysAuditLogMapper.selectByConditions(
                page,
                query.getTargetModule(),
                query.getOperationType(),
                query.getOperatorId(),
                query.getOperatorName(),
                query.getStartTime(),
                query.getEndTime(),
                query.getKeyword(),
                query.getTargetRecordId()
        );
    }

    public AuditLogDetailDTO getDetail(Long id) {
        SysAuditLog log = sysAuditLogMapper.selectById(id);
        if (log == null) {
            return null;
        }
        AuditLogDetailDTO detail = new AuditLogDetailDTO();
        detail.setLog(log);
        detail.setDiffs(computeDiffs(log));
        return detail;
    }

    private List<FieldDiffDTO> computeDiffs(SysAuditLog log) {
        List<FieldDiffDTO> result = new ArrayList<>();
        List<Map<String, Object>> rawDiffs = AuditLogUtil.compareJson(
                log.getBeforeSnapshot(),
                log.getAfterSnapshot()
        );
        for (Map<String, Object> raw : rawDiffs) {
            FieldDiffDTO diff = new FieldDiffDTO();
            diff.setFieldName((String) raw.get("fieldName"));
            diff.setFieldLabel(AuditLogUtil.convertToFieldLabel((String) raw.get("fieldName")));
            diff.setOldValue(raw.get("oldValue"));
            diff.setNewValue(raw.get("newValue"));
            diff.setDiffType((String) raw.get("diffType"));
            result.add(diff);
        }
        return result;
    }

    public List<SysAuditLog> getOperatorTrail(Long operatorId) {
        return sysAuditLogMapper.selectByOperatorIdOrderByTime(operatorId);
    }

    public List<SysAuditLog> getRecordHistory(String targetModule, String targetRecordId) {
        return sysAuditLogMapper.selectByTargetRecordOrderByTime(targetModule, targetRecordId);
    }

    public int archiveLogs(LocalDateTime cutoffTime) {
        return sysAuditLogMapper.archiveOldLogs(cutoffTime);
    }

    public int archiveLogsOlderThanOneYear() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusYears(1);
        return archiveLogs(cutoffTime);
    }
}
