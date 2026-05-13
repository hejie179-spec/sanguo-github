package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("event")
public class Event {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String type;
    private String dynasty;
    private String content;
    private String historicalValue;
    private String imageUrl;
    private Integer sort;
}
