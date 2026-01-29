package com.authvs.server.service;

import com.authvs.server.entity.OauthClient;
import com.authvs.server.mapper.OauthClientMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final OauthClientMapper oauthClientMapper;

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Save operation is not supported yet.");
    }

    @Override
    public RegisteredClient findById(String id) {
        OauthClient oauthClient = oauthClientMapper.selectById(id);
        if (oauthClient == null) {
            return null;
        }
        return toRegisteredClient(oauthClient);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        OauthClient oauthClient = oauthClientMapper.selectOne(new LambdaQueryWrapper<OauthClient>()
                .eq(OauthClient::getClientId, clientId));
        if (oauthClient == null) {
            return null;
        }
        return toRegisteredClient(oauthClient);
    }

    private RegisteredClient toRegisteredClient(OauthClient oauthClient) {
        if (oauthClient.getEnabled() == null || oauthClient.getEnabled() == 0) {
            return null; // Or throw exception, but returning null usually means not found
        }

        // Set<String> clientAuthenticationMethods = Set.of("client_secret_basic", "client_secret_post");
        Set<String> authorizationGrantTypes = StringUtils.commaDelimitedListToSet(oauthClient.getGrantTypes());
        Set<String> redirectUris = StringUtils.commaDelimitedListToSet(oauthClient.getRedirectUris());
        Set<String> postLogoutRedirectUris = StringUtils.commaDelimitedListToSet(oauthClient.getPostLogoutRedirectUris());
        Set<String> clientScopes = StringUtils.commaDelimitedListToSet(oauthClient.getScopes());

        RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(oauthClient.getId()))
                .clientId(oauthClient.getClientId())
                .clientSecret(oauthClient.getClientSecret())
                .clientName(oauthClient.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE); // Support public clients if needed

        // Grant Types
        for (String grantType : authorizationGrantTypes) {
            if ("authorization_code".equals(grantType)) {
                builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            } else if ("refresh_token".equals(grantType)) {
                builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
            } else if ("client_credentials".equals(grantType)) {
                builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
            }
        }

        // Redirect URIs
        for (String uri : redirectUris) {
            builder.redirectUri(uri);
        }

        // Post Logout Redirect URIs
        for (String uri : postLogoutRedirectUris) {
            builder.postLogoutRedirectUri(uri);
        }

        // Scopes
        for (String scope : clientScopes) {
            builder.scope(scope);
        }

        // Token Settings
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
        if (oauthClient.getAccessTokenValidity() != null) {
            tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(oauthClient.getAccessTokenValidity()));
        }
        if (oauthClient.getRefreshTokenValidity() != null) {
            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(oauthClient.getRefreshTokenValidity()));
        }
        // ID Token validity usually follows access token or configured separately. 
        // Spring Auth Server TokenSettings doesn't have explicit idTokenTimeToLive in standard builder easily without custom customization, 
        // but it does control access/refresh. ID Token usually matches access token or default.
        // Let's assume ID Token validity isn't directly exposed in basic TokenSettings builder in older versions, 
        // but in newer versions it might be. Let's stick to standard ones.
        // Actually, newer versions do not explicitly have idTokenTimeToLive in TokenSettings. 
        // It's derived. But we can set the others.

        builder.tokenSettings(tokenSettingsBuilder.build());

        // Client Settings
        ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
        // Auto approve: 0-need confirm, 1-auto. 
        // requireAuthorizationConsent: true -> need confirm. 
        // So if auto_approve == 1, requireAuthorizationConsent = false.
        boolean requireConsent = oauthClient.getAutoApprove() == null || oauthClient.getAutoApprove() == 0;
        clientSettingsBuilder.requireAuthorizationConsent(requireConsent);

        builder.clientSettings(clientSettingsBuilder.build());

        return builder.build();
    }
}
