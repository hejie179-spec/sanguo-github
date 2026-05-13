package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_reply")
public class ForumReply {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer topicId;
    private Integer userId;
    private String content;
    private Integer parentId;
    private Integer status;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
