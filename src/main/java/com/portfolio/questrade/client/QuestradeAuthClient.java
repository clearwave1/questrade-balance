package com.portfolio.questrade.client;

import com.portfolio.questrade.dto.TokenResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * login.questrade.com never changes, so this is a normal CDI-managed rest client
 * (config key "questrade-auth" -> quarkus.rest-client.questrade-auth.url).
 */
@RegisterRestClient(configKey = "questrade-auth")
public interface QuestradeAuthClient {

    @GET
    @Path("/oauth2/token")
    TokenResponse getToken(@QueryParam("grant_type") String grantType,
                            @QueryParam("refresh_token") String refreshToken);
}
