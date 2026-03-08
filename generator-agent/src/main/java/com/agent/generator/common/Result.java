package com.agent.generator.common;

import lombok.Data;

@Data
public class Result<T> {
    private boolean success;
    private String error;
    private T data;

    // 成功返回
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    // 失败返回
    public static <T> Result<T> fail(String error) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
}