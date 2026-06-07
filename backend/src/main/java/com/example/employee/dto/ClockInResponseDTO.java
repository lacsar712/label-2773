package com.example.employee.dto;

import com.example.employee.entity.AttendanceRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClockInResponseDTO {
    private Boolean success;

    private String message;

    private AttendanceRecord record;
}
