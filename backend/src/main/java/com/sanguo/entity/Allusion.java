package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("allusion")
public class Allusion {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String source;
    private String content;
    private String imageUrl;
    private Integer sort;
}
