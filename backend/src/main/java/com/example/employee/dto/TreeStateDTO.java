package com.example.employee.dto;

import lombok.Data;

import java.util.List;

@Data
public class TreeStateDTO {
    private String treeKey;

    private List<Long> expandedIds;

    private Long selectedId;
}
