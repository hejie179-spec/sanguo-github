package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("person")
public class Person {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String alias;
    private String dynasty;
    private String introduction;
    private String imageUrl;
    private Integer sort;
}
