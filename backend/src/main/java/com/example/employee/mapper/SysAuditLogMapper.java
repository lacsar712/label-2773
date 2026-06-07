package com.example.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.employee.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    IPage<SysAuditLog> selectByConditions(
            Page<SysAuditLog> page,
            @Param("targetModule") String targetModule,
            @Param("operationType") Integer operationType,
            @Param("operatorId") Long operatorId,
            @Param("operatorName") String operatorName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            @Param("targetRecordId") String targetRecordId
    );

    List<SysAuditLog> selectByOperatorIdOrderByTime(@Param("operatorId") Long operatorId);

    List<SysAuditLog> selectByTargetRecordOrderByTime(
            @Param("targetModule") String targetModule,
            @Param("targetRecordId") String targetRecordId
    );

    int archiveOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);
}
