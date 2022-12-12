package com.example.androidLIS.model;

import lombok.Data;

@Data
public class ResponseData<T> {
    public String status;
    public String message;
    public String error;
    public T result;
    public T data;
    public String timeStamp;
}
