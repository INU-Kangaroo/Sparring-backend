package com.kangaroo.sparring.global.security.oauth2.service.provider;

import com.kangaroo.sparring.domain.user.dto.req.OAuth2CodeRequest;

import java.util.Map;

public interface OAuth2ProviderClient {

    String getProvider();

    String resolveAccessToken(OAuth2CodeRequest request);

    Map<String, Object> fetchUserInfo(String accessToken);
}
