package com.authvs.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("oauth_clients")
public class OauthClient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String clientId;

    private String clientSecret;

    private String clientName;

    private String grantTypes;

    private String redirectUris;

    private String postLogoutRedirectUris;

    private Integer accessTokenValidity;

    private Integer refreshTokenValidity;

    private Integer idTokenValidity;

    private String scopes;

    private Integer autoApprove; // 0 or 1

    private Integer clientType;

    private String description;

    private String logoUrl;

    private String homeUrl;

    private Integer enabled; // 0 or 1

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
