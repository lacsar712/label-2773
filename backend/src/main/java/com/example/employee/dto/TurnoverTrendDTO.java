package com.example.employee.dto;

import lombok.Data;

@Data
public class TurnoverTrendDTO {
    private String month;
    private Long hires;
    private Long departures;
    private Double attritionRate;
}
