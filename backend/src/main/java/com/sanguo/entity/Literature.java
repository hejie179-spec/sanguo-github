package com.sanguo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("literature")
public class Literature {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String author;
    private String publishYear;
    private String category;
    private String source;
    private String imageUrl;
    private Integer sort;
}
