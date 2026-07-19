package com.portfolio.questrade.service;

import com.portfolio.questrade.client.QuestradeApiClient;
import com.portfolio.questrade.client.QuestradeAuthClient;
import com.portfolio.questrade.dto.TokenResponse;
import com.portfolio.questrade.entity.QuestradeConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.time.Instant;

@ApplicationScoped
public class QuestradeTokenService {

    // Refresh a bit before actual expiry to avoid racing a request against a token that
    // dies mid-flight. Questrade access tokens are typically valid ~30 minutes.
    private static final long EXPIRY_BUFFER_SECONDS = 90;

    @Inject
    @RestClient
    QuestradeAuthClient authClient;

    /**
     * Returns a valid access token for the connection, refreshing first if needed.
     * Always re-reads the connection's current token state, since it may have been
     * rotated by a concurrent call.
     */
    public String getValidAccessToken(QuestradeConnection connection) {
        if (needsRefresh(connection)) {
            refresh(connection);
        }
        return connection.accessToken;
    }

    private boolean needsRefresh(QuestradeConnection connection) {
        return connection.accessToken == null
                || connection.tokenExpiresAt == null
                || Instant.now().isAfter(connection.tokenExpiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    /**
     * Exchanges the current refresh token for a new access token + a NEW refresh token.
     * Questrade invalidates the old refresh token as soon as it's used, so the new one is
     * persisted immediately - if this fails partway through, the connection needs a fresh
     * manual token from Questrade App Hub.
     */
    @Transactional
    public void refresh(QuestradeConnection connection) {
        TokenResponse response;
        try {
            response = authClient.getToken("refresh_token", connection.refreshToken);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to refresh Questrade token for connection '" + connection.label
                            + "'. The refresh token may be expired or already used - generate a new one "
                            + "from Questrade App Hub (login.questrade.com > API Access) and re-register "
                            + "the connection.", e);
        }

        connection.accessToken = response.accessToken;
        connection.apiServer = response.apiServer;
        connection.refreshToken = response.refreshToken; // rotate immediately - old one is now dead
        connection.tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn);
        connection.lastRefreshedAt = Instant.now();
        QuestradeConnection.getEntityManager().merge(connection);
    }

    /**
     * Builds a REST client pointed at this connection's api_server. Cheap to build;
     * not cached since api_server can change on refresh.
     */
    public QuestradeApiClient apiClientFor(QuestradeConnection connection) {
        if (connection.apiServer == null) {
            throw new IllegalStateException(
                    "Connection '" + connection.label + "' has no api_server yet - refresh it first.");
        }
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(connection.apiServer))
                .build(QuestradeApiClient.class);
    }
}