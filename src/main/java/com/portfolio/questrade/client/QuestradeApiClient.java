package com.portfolio.questrade.client;

import com.portfolio.questrade.dto.AccountsResponse;
import com.portfolio.questrade.dto.BalancesResponse;
import com.portfolio.questrade.dto.PositionsResponse;
import com.portfolio.questrade.dto.SymbolsResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/**
 * Questrade's api_server (e.g. https://api01.iq.questrade.com/) is returned per-login at
 * token-refresh time and can differ between connections, so this interface is NOT
 * @RegisterRestClient - it's built at runtime with a dynamic base URI
 * (see QuestradeTokenService#apiClientFor).
 */
public interface QuestradeApiClient {

    @GET
    @Path("/v1/accounts")
    AccountsResponse getAccounts(@HeaderParam("Authorization") String bearerToken);

    @GET
    @Path("/v1/accounts/{id}/balances")
    BalancesResponse getBalances(@HeaderParam("Authorization") String bearerToken,
                                  @PathParam("id") String accountNumber);

    @GET
    @Path("/v1/accounts/{id}/positions")
    PositionsResponse getPositions(@HeaderParam("Authorization") String bearerToken,
                                    @PathParam("id") String accountNumber);

    @GET
    @Path("/v1/symbols")
    SymbolsResponse getSymbols(@HeaderParam("Authorization") String bearerToken,
                                @QueryParam("ids") String commaSeparatedIds);
}
