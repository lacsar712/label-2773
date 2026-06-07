package com.example.employee.dto;

import com.example.employee.entity.SysAuditLog;
import lombok.Data;

import java.util.List;

@Data
public class AuditLogDetailDTO {

    private SysAuditLog log;

    private List<FieldDiffDTO> diffs;
}
