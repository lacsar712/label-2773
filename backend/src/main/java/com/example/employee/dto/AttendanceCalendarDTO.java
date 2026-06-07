package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceCalendarDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attendanceDate;

    private Integer status;

    private Integer lateMinutes;

    private Integer earlyMinutes;
}
