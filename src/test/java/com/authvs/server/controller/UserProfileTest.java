package com.authvs.server.controller;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setup() {
        // Ensure user exists for the mock user
        User user = new User();
        user.setUsername("test-user");
        user.setPassword("password");
        user.setEmail("test@example.com");
        user.setUserType(1);
        user.setEnabled(1);
        userMapper.insert(user);
    }

    @Test
    @WithMockUser(username = "test-user")
    void profile_WhenLoggedIn_ShouldReturnProfileView() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }

    @Test
    void profile_WhenNotLoggedIn_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().is3xxRedirection());
    }
}
