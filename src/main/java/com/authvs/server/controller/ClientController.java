package com.authvs.server.controller;

import com.authvs.server.entity.OauthClient;
import com.authvs.server.mapper.OauthClientMapper;
import com.authvs.server.model.out.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ClientController {

    private final OauthClientMapper oauthClientMapper;

    @GetMapping("/api/oauth2/client/{clientId}")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getClientInfo(@PathVariable String clientId) {
        OauthClient client = oauthClientMapper.selectOne(new LambdaQueryWrapper<OauthClient>()
                .eq(OauthClient::getClientId, clientId));

        if (client == null) {
            return R.fail("Client not found");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("clientName", client.getClientName());
        info.put("logoUrl", client.getLogoUrl());
        info.put("description", client.getDescription());
        // Do not return sensitive info like secret

        return R.ok(info);
    }
}
