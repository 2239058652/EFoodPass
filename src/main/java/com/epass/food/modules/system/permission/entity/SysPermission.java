package com.epass.food.modules.system.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("sys_permission")
@Data
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private String method;
    private Integer status;
}