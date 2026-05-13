package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("event_allusion")
public class EventAllusion {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer eventId;
    private Integer allusionId;
}
