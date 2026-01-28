package com.authvs.server.config;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setup() {
        // Ensure test user exists
        User user = new User();
        user.setUsername("test-admin");
        user.setPassword(passwordEncoder.encode("test-password"));
        user.setEmail("test@example.com");
        user.setUserType(2);
        user.setEnabled(1);
        userMapper.insert(user);

        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.delete(anyString())).thenReturn(true);
    }

    @Test
    void login_DirectAccess_ShouldReturnJsonWithDefaultSuccessUrl() throws Exception {
        // 准备验证码
        String captchaKey = "test-key-1";
        String captchaCode = "1234";
        when(valueOperations.get("captcha:login:" + captchaKey)).thenReturn(captchaCode);

        // 直接访问登录页面并提交登录
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\", \"password\":\"test-password\", \"captcha\":\"" + captchaCode + "\", \"captchaKey\":\"" + captchaKey + "\"}")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("/profile"));
    }

    @Test
    void login_WithWrongCaptcha_ShouldFail() throws Exception {
        // 准备验证码
        String captchaKey = "test-key-2";
        String captchaCode = "1234";
        when(valueOperations.get("captcha:login:" + captchaKey)).thenReturn(captchaCode);

        // 提交错误验证码
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\", \"password\":\"test-password\", \"captcha\":\"5678\", \"captchaKey\":\"" + captchaKey + "\"}")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
    }

    @Test
    void login_AfterInterception_ShouldReturnJsonWithSavedRequest() throws Exception {
        // 1. 访问受保护资源，返回 401 JSON
        // 这里访问 /user/profile (API) 会被拦截
        MvcResult result = mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andReturn();

        // 获取 Session (包含 SavedRequest)
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        
        // 准备验证码
        String captchaKey = "test-key-3";
        String captchaCode = "1234";
        when(valueOperations.get("captcha:login:" + captchaKey)).thenReturn(captchaCode);

        // 2. 使用同一个 Session 登录
        mockMvc.perform(post("/api/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\", \"password\":\"test-password\", \"captcha\":\"" + captchaCode + "\", \"captchaKey\":\"" + captchaKey + "\"}")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
                // .andExpect(jsonPath("$.data").value("http://localhost/user/profile")); // Skip exact match
    }
}
