package com.linxi.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果类
 */
@Data
public class PageResult<T> {

    private long total;
    private long pages;
    private long pageNo;
    private long pageSize;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, long pages, long pageNo, long pageSize, List<T> records) {
        this.total = total;
        this.pages = pages;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, long pages, long pageNo, long pageSize, List<T> records) {
        return new PageResult<>(total, pages, pageNo, pageSize, records);
    }
}
