package com.example.employee.dto;

import lombok.Data;

@Data
public class FieldDiffDTO {

    private String fieldName;

    private String fieldLabel;

    private Object oldValue;

    private Object newValue;

    private String diffType;
}
