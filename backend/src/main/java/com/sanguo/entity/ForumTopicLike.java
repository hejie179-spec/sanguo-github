package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("forum_topic_like")
public class ForumTopicLike {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer topicId;
    private Integer userId;
}
