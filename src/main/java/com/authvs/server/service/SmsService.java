package com.authvs.server.service;

import cn.emay.sdk.client.SmsSDKClient;
import cn.emay.sdk.core.dto.sms.common.ResultModel;
import cn.emay.sdk.core.dto.sms.request.SmsSingleRequest;
import cn.emay.sdk.core.dto.sms.response.SmsResponse;
import cn.emay.sdk.util.http.common.EmayHttpResultCode;
import com.authvs.server.config.SmsConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    private final SmsConfig smsConfig;

    private SmsSDKClient client;

    @PostConstruct
    public void init() {
        try {
            this.client = new SmsSDKClient(smsConfig.getIp(), smsConfig.getPort(), smsConfig.getAppId(), smsConfig.getSecretKey());
        } catch (Exception e) {
            log.error("初始化短信客户端失败", e);
        }
    }

    @Async
    public void sendSms(String mobile, String content) {
        if (client == null) {
            log.error("短信客户端未初始化，无法发送短信: {} -> {}", mobile, content);
            return;
        }
        try {
            // 拼接签名
            String fullContent = smsConfig.getSign() + content;
            SmsSingleRequest request = new SmsSingleRequest(mobile, fullContent, null, null, "");
            ResultModel<SmsResponse> result = client.sendSingleSms(request);
            if (result.getCode().equals(EmayHttpResultCode.SUCCESS.getCode())) {
                SmsResponse response = result.getResult();
                log.info("短信发送成功: {} -> {}, response: {}", mobile, content, response.getSmsId());
            } else {
                log.error("短信发送失败: {} -> {}, code: {}, result: {}", mobile, content, result.getCode(), result.getResult());
            }
        } catch (Exception e) {
            log.error("短信发送异常: {} -> {}", mobile, content, e);
        }
    }

    @Async
    public void sendLoginCodeSms(String mobile, String code, int expiresMinutes) {
        String content = "您的登录验证码是：" + code + "，有效期" + expiresMinutes + "分钟。如非本人操作，请忽略。";
        sendSms(mobile, content);
    }
}
