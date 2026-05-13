package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private Integer userId;
    private String category;
    private String content;
    private String coverUrl;
    private Integer status;
    private String reason;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
