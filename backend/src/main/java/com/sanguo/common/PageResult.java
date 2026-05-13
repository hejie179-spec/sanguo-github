package com.sanguo.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long current, long size, List<T> records) {
        PageResult<T> p = new PageResult<>();
        p.setTotal(total);
        p.setPages((total + size - 1) / size);
        p.setCurrent(current);
        p.setSize(size);
        p.setRecords(records);
        return p;
    }
}
