package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("person_event")
public class PersonEvent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer personId;
    private Integer eventId;
}
