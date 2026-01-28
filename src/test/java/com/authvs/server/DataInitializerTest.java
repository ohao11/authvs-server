package com.authvs.server;

import com.authvs.server.entity.OauthClient;
import com.authvs.server.entity.User;
import com.authvs.server.mapper.OauthClientMapper;
import com.authvs.server.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootTest
class DataInitializerTest {

    @Autowired
    private OauthClientMapper oauthClientMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void initData() {
        // Init User
        if (userMapper.selectCount(null) == 0) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("password"));
            user.setEmail("admin@example.com");
            user.setPhone("13800138000");
            user.setUserType(2); // ADMIN
            user.setEnabled(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
            System.out.println("Initialized User: admin / password");
        }

        // Init Client
        if (oauthClientMapper.selectCount(null) == 0) {
            OauthClient client = new OauthClient();
            client.setClientId("oidc-client");
            client.setClientSecret(passwordEncoder.encode("secret"));
            client.setClientName("OIDC Demo Client");
            client.setGrantTypes("authorization_code,refresh_token,client_credentials");
            client.setRedirectUris("http://127.0.0.1:8080/login/oauth2/code/oidc-client,https://oauth.pstmn.io/v1/callback");
            client.setPostLogoutRedirectUris("http://127.0.0.1:8080");
            client.setScopes("openid,profile,email,phone");
            client.setAutoApprove(0); // Require consent
            client.setClientType(1); // Web
            client.setEnabled(1);
            client.setAccessTokenValidity(3600);
            client.setRefreshTokenValidity(3600 * 24 * 30);
            client.setIdTokenValidity(3600);
            client.setCreatedAt(LocalDateTime.now());
            client.setUpdatedAt(LocalDateTime.now());
            oauthClientMapper.insert(client);
            System.out.println("Initialized Client: oidc-client / secret");
        }
    }
}
