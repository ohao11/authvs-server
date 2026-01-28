package com.authvs.server.service;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserInfoServiceTest {

    @Mock
    private UserMapper userMapper;

    private CustomOidcUserInfoService userInfoService;

    @BeforeEach
    void setUp() {
        userInfoService = new CustomOidcUserInfoService(userMapper);
    }

    @Test
    void apply_ShouldReturnUserInfoWithRequestedScopes() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setPhone("1234567890");
        user.setUserType(1);
        
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        OAuth2Authorization authorization = mock(OAuth2Authorization.class);
        when(authorization.getPrincipalName()).thenReturn(username);
        
        Set<String> authorizedScopes = new HashSet<>();
        authorizedScopes.add("openid");
        authorizedScopes.add("profile");
        authorizedScopes.add("email");
        authorizedScopes.add("phone");
        when(authorization.getAuthorizedScopes()).thenReturn(authorizedScopes);

        OidcUserInfoAuthenticationContext context = mock(OidcUserInfoAuthenticationContext.class);
        when(context.getAuthorization()).thenReturn(authorization);

        // Act
        OidcUserInfo userInfo = userInfoService.apply(context);

        // Assert
        assertNotNull(userInfo);
        assertEquals(username, userInfo.getSubject());
        assertEquals(username, userInfo.getPreferredUsername());
        assertEquals("test@example.com", userInfo.getEmail());
        assertEquals("1234567890", userInfo.getPhoneNumber());
        assertEquals(Integer.valueOf(1), userInfo.getClaim("user_type"));
    }

    @Test
    void apply_ShouldReturnOnlySubject_WhenOnlyOpenIdScope() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        OAuth2Authorization authorization = mock(OAuth2Authorization.class);
        when(authorization.getPrincipalName()).thenReturn(username);
        
        Set<String> authorizedScopes = new HashSet<>();
        authorizedScopes.add("openid");
        when(authorization.getAuthorizedScopes()).thenReturn(authorizedScopes);

        OidcUserInfoAuthenticationContext context = mock(OidcUserInfoAuthenticationContext.class);
        when(context.getAuthorization()).thenReturn(authorization);

        // Act
        OidcUserInfo userInfo = userInfoService.apply(context);

        // Assert
        assertNotNull(userInfo);
        assertEquals(username, userInfo.getSubject());
        assertNull(userInfo.getPreferredUsername());
        assertNull(userInfo.getEmail());
    }
}
