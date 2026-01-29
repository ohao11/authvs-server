package com.authvs.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    private String phone;

    private Integer userType; // 1-PORTAL, 2-ADMIN

    private String totpSecret;

    private Integer totpEnabled; // 0 or 1

    private Integer enabled; // 0 or 1

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
