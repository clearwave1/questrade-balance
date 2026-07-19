package com.portfolio.questrade.service;

import com.portfolio.questrade.entity.QuestradeConnection;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class TokenRefreshScheduler {

    private static final Logger LOG = Logger.getLogger(TokenRefreshScheduler.class);

    @Inject
    QuestradeTokenService tokenService;

    @Scheduled(every = "20m")
    @Transactional
    public void refreshAllTokens() {
        List<QuestradeConnection> connections = QuestradeConnection.listAllConnections();
        if (connections.isEmpty()) {
            return;
        }
        for (QuestradeConnection connection : connections) {
            try {
                tokenService.refresh(connection);
                LOG.infof("Token refreshed for connection '%s', next api_server: %s",
                        connection.label, connection.apiServer);
            } catch (Exception e) {
                LOG.errorf("Failed to refresh token for connection '%s': %s — " +
                                "generate a new token from Questrade App Hub and PUT /connections/%s",
                        connection.label, e.getMessage(), connection.label);
            }
        }
    }
}