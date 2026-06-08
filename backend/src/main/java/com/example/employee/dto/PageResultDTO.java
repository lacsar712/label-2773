package com.example.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultDTO<T> {

    private List<T> list;

    private List<T> records;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private Integer current;

    private Integer size;

    private Long pages;
}
