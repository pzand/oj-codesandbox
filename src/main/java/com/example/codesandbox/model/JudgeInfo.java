package com.example.codesandbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    public String message;

    /**
     * 消耗内存
     */
    public Long memory;

    /**
     * 消耗时间
     */
    public Long time;
}
