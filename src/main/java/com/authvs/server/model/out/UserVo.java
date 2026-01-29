package com.authvs.server.model.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String email;

    private String phone;

    private Integer userType;

    private Integer enabled;

    private Integer totpEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
