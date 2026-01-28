package com.authvs.server.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.authvs.server.model.enums.CaptchaType;
import com.authvs.server.model.out.CaptchaVo;
import com.authvs.server.model.out.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.authvs.server.model.in.CaptchaParam;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class CaptchaController {

    private final StringRedisTemplate redisTemplate;

    @PostMapping("/api/captcha")
    public R<CaptchaVo> captcha(@RequestBody CaptchaParam param) {
        String type = param.getType();
        
        // 校验验证码类型
        if (!CaptchaType.LOGIN.getType().equalsIgnoreCase(type)) {
            return R.fail("Unsupported captcha type");
        }

        // 生成 Key
        String key = UUID.randomUUID().toString();

        // 定义图形验证码的长和宽
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100);

        // 将验证码存入 Redis，有效期 5 分钟
        String redisKey = "captcha:" + type + ":" + key;
        redisTemplate.opsForValue().set(redisKey, lineCaptcha.getCode(), 5, TimeUnit.MINUTES);

        // 返回结果
        // image base64 data
        String imageBase64 = lineCaptcha.getImageBase64Data();
        
        return R.ok(new CaptchaVo(key, imageBase64));
    }
}
