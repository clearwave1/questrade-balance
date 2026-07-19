package com.portfolio.questrade.resource;

import com.portfolio.questrade.dto.request.CreateConnectionRequest;
import com.portfolio.questrade.entity.QuestradeConnection;
import com.portfolio.questrade.service.QuestradeTokenService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/connections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionResource {

    @Inject
    QuestradeTokenService tokenService;

    @GET
    public List<Map<String, Object>> list() {
        return QuestradeConnection.<QuestradeConnection>listAllConnections().stream()
                .map(c -> Map.<String, Object>of(
                        "label", c.label,
                        "apiServer", c.apiServer == null ? "" : c.apiServer,
                        "lastRefreshedAt", c.lastRefreshedAt == null ? "" : c.lastRefreshedAt.toString(),
                        "tokenExpiresAt", c.tokenExpiresAt == null ? "" : c.tokenExpiresAt.toString()
                ))
                .collect(Collectors.toList());
    }

    @POST
    @Transactional
    public Response create(CreateConnectionRequest request) {
        if (request.label == null || request.label.isBlank() || request.refreshToken == null || request.refreshToken.isBlank()) {
            throw new BadRequestException("label and refreshToken are required");
        }
        if (QuestradeConnection.findByLabel(request.label) != null) {
            throw new WebApplicationException("A connection with label '" + request.label + "' already exists. Use PUT /connections/{label} to update it.", 409);
        }

        QuestradeConnection connection = new QuestradeConnection();
        connection.label = request.label;
        connection.refreshToken = request.refreshToken;
        connection.persist();

        tokenService.refresh(connection);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("label", connection.label, "apiServer", connection.apiServer))
                .build();
    }

    /**
     * Update the refresh token for an existing connection - use this when the stored
     * refresh token has expired and you've generated a fresh one from Questrade App Hub.
     * Immediately exchanges the new token to confirm it works and capture the api_server.
     */
    @PUT
    @Path("/{label}")
    @Transactional
    public Response update(@PathParam("label") String label, CreateConnectionRequest request) {
        if (request.refreshToken == null || request.refreshToken.isBlank()) {
            throw new BadRequestException("refreshToken is required");
        }

        QuestradeConnection connection = QuestradeConnection.findByLabel(label);
        if (connection == null) {
            throw new NotFoundException("No connection found with label '" + label + "'. Use POST /connections to create it.");
        }

        // Clear the old cached access token so the next call is forced to use the new refresh token
        connection.refreshToken = request.refreshToken;
        connection.accessToken = null;
        connection.tokenExpiresAt = null;

        tokenService.refresh(connection);

        return Response.ok(Map.of("label", connection.label, "apiServer", connection.apiServer)).build();
    }

    @DELETE
    @Path("/{label}")
    @Transactional
    public Response delete(@PathParam("label") String label) {
        QuestradeConnection connection = QuestradeConnection.findByLabel(label);
        if (connection == null) {
            throw new NotFoundException();
        }
        connection.delete();
        return Response.noContent().build();
    }
}