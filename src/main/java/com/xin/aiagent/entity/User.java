package com.xin.aiagent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体（M0 极简版本）。
 *
 * 仅保留最小字段以打通认证闭环：用户名、密码（BCrypt）、邮箱、状态与审计时间。
 * 后续如需扩展昵称、头像、角色关联等，可在 M1/M2 阶段演进。
 */
@TableName("users")
@Data
public class User {
    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码密文（BCrypt） */
    private String password;

    /** 邮箱（唯一，可选） */
    private String email;

    /** 账号状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间（由实体钩子设置） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（由实体钩子设置） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
