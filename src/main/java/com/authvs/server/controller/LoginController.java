package com.authvs.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.authvs.server.model.enums.CaptchaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.RestController;
import com.authvs.server.model.in.LoginParam;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import com.authvs.server.model.out.R;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @PostMapping("/api/login")
    public R<String> login(@RequestBody LoginParam loginParam,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        String username = loginParam.getUsername();
        String password = loginParam.getPassword();
        String captcha = loginParam.getCaptcha();
        String captchaKey = loginParam.getCaptchaKey();

        // 0. 校验验证码
        String redisKey = "captcha:" + CaptchaType.LOGIN.getType() + ":" + captchaKey;
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
            // 1. 创建认证令牌
            UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
            
            // 2. 执行认证
            Authentication authentication = authenticationManager.authenticate(token);
            
            // 3. 创建并设置 SecurityContext
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);
            
            // 4. 持久化 SecurityContext 到 Session
            securityContextRepository.saveContext(context, request, response);
            
            log.info("用户 [{}] 自定义登录成功", username);

            // 5. 处理跳转逻辑 (优先跳转到 SavedRequest)
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                return R.ok(savedRequest.getRedirectUrl());
            }
            
            return R.ok("/profile"); // Default redirect to profile page
            
        } catch (AuthenticationException e) {
            log.warn("用户 [{}] 登录失败: {}", username, e.getMessage());
            return R.fail("用户名或密码错误");
        }
    }
}
