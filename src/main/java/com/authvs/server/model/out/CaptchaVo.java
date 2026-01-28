package com.authvs.server.model.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 验证码 Key (UUID)
     */
    private String key;

    /**
     * 验证码图片 (Base64)
     */
    private String image;
}
