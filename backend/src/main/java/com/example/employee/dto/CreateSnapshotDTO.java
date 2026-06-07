package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSnapshotDTO {
    @NotBlank(message = "快照名称不能为空")
    private String snapshotName;

    private String description;
}
