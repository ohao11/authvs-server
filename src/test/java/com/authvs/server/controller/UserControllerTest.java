package com.authvs.server.controller;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @Test
    @WithMockUser(username = "admin")
    void index_ShouldRedirectToProfile() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"));
    }

    @Test
    @WithMockUser(username = "admin")
    void profile_ShouldShowUserInfo() throws Exception {
        // Mock database response
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setPhone("13800138000");
        user.setUserType(2);
        user.setEnabled(1);
        user.setCreatedAt(LocalDateTime.now());
        
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("用户详情")))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("admin@example.com")))
                .andExpect(content().string(containsString("13800138000")));
    }
    
    @Test
    void profile_ShouldRedirectToLogin_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }
}
