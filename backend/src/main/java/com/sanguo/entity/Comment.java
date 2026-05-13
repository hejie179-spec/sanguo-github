package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer targetType;
    private Integer targetId;
    private String content;
    private Integer parentId;
    private Integer status;
    private LocalDateTime createTime;
}
