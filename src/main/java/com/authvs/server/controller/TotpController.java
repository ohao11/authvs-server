package com.authvs.server.controller;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import com.authvs.server.model.in.TotpDisableParam;
import com.authvs.server.model.in.TotpEnableParam;
import com.authvs.server.model.out.R;
import com.authvs.server.model.out.TotpSetupVo;
import com.authvs.server.util.TotpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TotpController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/api/totp/setup")
    @PreAuthorize("isAuthenticated()")
    public R<TotpSetupVo> setup() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        GoogleAuthenticatorKey credentials = TotpUtil.createCredentials();
        String secret = credentials.getKey();
        String otpAuthUrl = TotpUtil.getQrCodeUrl("AuthVs", username, credentials);

        return R.ok(TotpSetupVo.builder()
                .secret(secret)
                .otpAuthUrl(otpAuthUrl)
                .build());
    }

    @PostMapping("/api/totp/enable")
    @PreAuthorize("isAuthenticated()")
    public R<Void> enable(@RequestBody TotpEnableParam param) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String secret = param.getSecret();
        String code = param.getCode();

        if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
            return R.fail("参数不完整");
        }

        // 验证验证码是否匹配该密钥
        if (!TotpUtil.authorize(secret, code)) {
            return R.fail("验证码错误");
        }

        // 更新用户表
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUsername, username)
                .set(User::getTotpSecret, secret)
                .set(User::getTotpEnabled, 1));

        log.info("用户 [{}] 开启双因子认证", username);
        return R.ok();
    }

    @PostMapping("/api/totp/disable")
    @PreAuthorize("isAuthenticated()")
    public R<Void> disable(@RequestBody TotpDisableParam param) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String password = param.getPassword();

        // 校验密码
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return R.fail("用户不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return R.fail("密码错误");
        }

        // 更新用户表
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUsername, username)
                .set(User::getTotpSecret, null)
                .set(User::getTotpEnabled, 0));

        log.info("用户 [{}] 关闭双因子认证", username);
        return R.ok();
    }
}
