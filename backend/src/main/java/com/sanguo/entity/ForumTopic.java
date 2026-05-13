package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_topic")
public class ForumTopic {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private Integer userId;
    private String category;
    private String content;
    private String coverUrl;
    private Integer status;
    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer topStatus;
    private Integer essenceStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
