package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_question_resource")
public class AiQuestionResource {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer questionId;
    private Integer targetType;
    private Integer targetId;
    private LocalDateTime createTime;
}
