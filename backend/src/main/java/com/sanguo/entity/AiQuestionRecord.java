package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_question_record")
public class AiQuestionRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String conversationId;
    private Integer turnNo;
    private String questionContent;
    private Integer questionType;
    private String answerContent;
    private LocalDateTime createTime;
}
