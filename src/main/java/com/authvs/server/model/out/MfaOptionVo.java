package com.authvs.server.model.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaOptionVo implements Serializable {
    private String type; // TOTP, SMS, EMAIL
    private String label;
    private String target; // 脱敏后的手机号或邮箱
}
