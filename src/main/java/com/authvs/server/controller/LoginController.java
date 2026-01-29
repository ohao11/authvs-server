package com.authvs.server.controller;

import cn.hutool.core.util.RandomUtil;
import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import com.authvs.server.model.enums.CaptchaType;
import com.authvs.server.model.in.LoginParam;
import com.authvs.server.model.in.MfaLoginParam;
import com.authvs.server.model.in.MfaSendParam;
import com.authvs.server.model.out.LoginVo;
import com.authvs.server.model.out.MfaOptionVo;
import com.authvs.server.model.out.R;
import com.authvs.server.service.EmailService;
import com.authvs.server.service.SmsService;
import com.authvs.server.service.VerificationRateLimiter;
import com.authvs.server.util.RedisKeys;
import com.authvs.server.util.TotpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final VerificationRateLimiter rateLimiter;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @PostMapping("/api/login")
    public R<LoginVo> login(@RequestBody LoginParam loginParam,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        String username = loginParam.getUsername();
        String password = loginParam.getPassword();
        String captcha = loginParam.getCaptcha();
        String captchaKey = loginParam.getCaptchaKey();

        // 0. 校验验证码
        String redisKey = RedisKeys.captcha(CaptchaType.LOGIN.getType(), captchaKey);
        String redisCaptcha = redisTemplate.opsForValue().get(redisKey);

        // 无论校验成功与否，删除 Redis 中的验证码，防止重放
        if (StringUtils.hasText(captchaKey)) {
            redisTemplate.delete(redisKey);
        }

        if (!StringUtils.hasText(captcha) || !captcha.equalsIgnoreCase(redisCaptcha)) {
            log.warn("用户 [{}] 验证码错误", username);
            return R.fail("验证码错误或已过期");
        }

        try {
            // 1. 验证用户名密码 (不创建 Session)
            UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
            Authentication authentication = authenticationManager.authenticate(token);

            // 2. 检查是否有 MFA 选项
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));

            List<MfaOptionVo> options = new ArrayList<>();
            if (user != null) {
                // TOTP
                if (user.getTotpEnabled() != null && user.getTotpEnabled() == 1) {
                    options.add(new MfaOptionVo("TOTP", "Google 验证器", null));
                }
                // EMAIL
                if (StringUtils.hasText(user.getEmail())) {
                    options.add(new MfaOptionVo("EMAIL", "邮箱验证", maskEmail(user.getEmail())));
                }
                // SMS
                if (StringUtils.hasText(user.getPhone())) {
                    options.add(new MfaOptionVo("SMS", "手机短信", maskPhone(user.getPhone())));
                }
            }

            if (!options.isEmpty()) {
                String mfaId = UUID.randomUUID().toString();
                // 存储临时登录态 5分钟有效
                redisTemplate.opsForValue().set(RedisKeys.mfaLogin(mfaId), username, 5, TimeUnit.MINUTES);

                return R.ok(LoginVo.builder()
                        .state("MFA_REQUIRED")
                        .mfaId(mfaId)
                        .options(options)
                        .build());
            }

            // 3. 无 MFA，直接登录成功
            return processLoginSuccess(authentication, request, response);

        } catch (AuthenticationException e) {
            log.warn("用户 [{}] 登录失败: {}", username, e.getMessage());
            return R.fail("用户名或密码错误");
        }
    }

    @PostMapping("/api/login/mfa")
    public R<LoginVo> mfaLogin(@RequestBody MfaLoginParam mfaLoginParam,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        String mfaId = mfaLoginParam.getMfaId();
        String code = mfaLoginParam.getCode();
        String type = mfaLoginParam.getType();

        String username = redisTemplate.opsForValue().get(RedisKeys.mfaLogin(mfaId));
        if (!StringUtils.hasText(username)) {
            return R.fail("登录会话已过期，请重新登录");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return R.fail("用户不存在");
        }

        boolean verified = false;
        if ("TOTP".equalsIgnoreCase(type)) {
            if (user.getTotpEnabled() != null && user.getTotpEnabled() == 1) {
                verified = TotpUtil.authorize(user.getTotpSecret(), code);
            }
        } else if ("SMS".equalsIgnoreCase(type) || "EMAIL".equalsIgnoreCase(type)) {
            String redisKey = RedisKeys.mfaCode(type, mfaId);
            String savedCode = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.hasText(savedCode) && savedCode.equals(code)) {
                verified = true;
                redisTemplate.delete(redisKey);
            }
        }

        if (!verified) {
            return R.fail("验证码错误");
        }

        // 验证成功，移除临时 mfaId
        redisTemplate.delete(RedisKeys.mfaLogin(mfaId));

        // 重新加载用户信息并构建 Authentication
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());

        return processLoginSuccess(authentication, request, response);
    }

    // 辅助方法
    private R<LoginVo> processLoginSuccess(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        // 3. 创建并设置 SecurityContext
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);

        // 4. 持久化 SecurityContext 到 Session (Redis)
        securityContextRepository.saveContext(context, request, response);

        log.info("用户 [{}] 登录成功", authentication.getName());

        // 获取 Session ID 作为 Token
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();

        // 5. 处理跳转逻辑 (优先跳转到 SavedRequest)
        String redirectUrl = "/profile";
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            redirectUrl = savedRequest.getRedirectUrl();
        }

        return R.ok(LoginVo.builder()
                .state("LOGGED_IN")
                .token(sessionId)
                .redirectUrl(redirectUrl)
                .build());
    }

    @PostMapping("/api/login/mfa/send")
    public R<Void> sendMfaCode(@RequestBody MfaSendParam param) {
        String mfaId = param.getMfaId();
        String type = param.getType(); // SMS, EMAIL

        String username = redisTemplate.opsForValue().get(RedisKeys.mfaLogin(mfaId));
        if (!StringUtils.hasText(username)) {
            return R.fail("会话已过期");
        }
        String target = null;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if ("EMAIL".equalsIgnoreCase(type)) {
            if (user != null && StringUtils.hasText(user.getEmail())) {
                target = user.getEmail();
            } else {
                log.warn("用户 [{}] 未绑定邮箱，无法发送验证码", username);
                return R.fail("未绑定邮箱");
            }
        } else if ("SMS".equalsIgnoreCase(type)) {
            if (user != null && StringUtils.hasText(user.getPhone())) {
                target = user.getPhone();
            } else {
                log.warn("用户 [{}] 未绑定手机号，无法发送验证码", username);
                return R.fail("未绑定手机号");
            }
        }
        String limitMsg = rateLimiter.checkAndRecord(type, target);
        if (limitMsg != null) {
            return R.fail(limitMsg);
        }
        String code = RandomUtil.randomNumbers(6);
        log.info("MFA验证码 [{} - {}]: {}", username, type, code);
        String redisKey = RedisKeys.mfaCode(type, mfaId);
        redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);
        if ("EMAIL".equalsIgnoreCase(type)) {
            emailService.sendLoginCodeMail(target, code, 5);
        } else if ("SMS".equalsIgnoreCase(type)) {
            smsService.sendLoginCodeSms(target, code, 5);
        }

        return R.ok();
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
