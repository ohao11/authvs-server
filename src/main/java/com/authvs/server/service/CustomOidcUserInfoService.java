package com.authvs.server.service;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CustomOidcUserInfoService implements Function<OidcUserInfoAuthenticationContext, OidcUserInfo> {

    private final UserMapper userMapper;

    @Override
    public OidcUserInfo apply(OidcUserInfoAuthenticationContext context) {
        // 获取当前认证信息中的用户名
        String username = context.getAuthorization().getPrincipalName();
        
        // 查询数据库获取用户详细信息
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        
        // 构建基础 UserInfo (subject 是必须的)
        OidcUserInfo.Builder userInfo = OidcUserInfo.builder()
                .subject(username);
                
        if (user == null) {
            return userInfo.build();
        }

        // 获取授权的 scopes
        Set<String> scopes = context.getAuthorization().getAuthorizedScopes();

        // 根据 scope 返回对应字段
        if (scopes.contains("profile")) {
            userInfo.preferredUsername(user.getUsername());
            if (user.getUserType() != null) {
                userInfo.claim("user_type", user.getUserType());
            }
            if (user.getUpdatedAt() != null) {
                userInfo.updatedAt(user.getUpdatedAt().toString());
            }
        }
        
        if (scopes.contains("email")) {
            userInfo.email(user.getEmail());
            userInfo.emailVerified(true); // 示例：假设已验证
        }
        
        if (scopes.contains("phone")) {
            userInfo.phoneNumber(user.getPhone());
            userInfo.phoneNumberVerified(true); // 示例：假设已验证
        }

        return userInfo.build();
    }
}
