package com.authvs.server.controller;

import com.authvs.server.entity.User;
import com.authvs.server.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

import com.authvs.server.model.out.R;
import com.authvs.server.model.out.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/api/user/profile")
    public R<UserVo> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (user != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            return R.ok(userVo);
        }
        return R.fail("用户不存在");
    }
}
