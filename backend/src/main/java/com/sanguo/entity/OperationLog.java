package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String module;
    private String operation;
    private String requestMethod;
    private String requestUrl;
    private String ip;
    private LocalDateTime createTime;
}
