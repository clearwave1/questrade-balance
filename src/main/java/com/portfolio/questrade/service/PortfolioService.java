package com.portfolio.questrade.service;

import com.portfolio.questrade.client.QuestradeApiClient;
import com.portfolio.questrade.dto.*;
import com.portfolio.questrade.dto.response.AccountCorrectedBalanceDto;
import com.portfolio.questrade.dto.response.ConnectionPortfolioDto;
import com.portfolio.questrade.dto.response.PortfolioSummaryDto;
import com.portfolio.questrade.entity.QuestradeConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@ApplicationScoped
public class PortfolioService
{

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String OPTION_SECURITY_TYPE = "Option";

    @Inject
    QuestradeTokenService tokenService;

    public PortfolioSummaryDto getCorrectedPortfolio()
    {
        List<QuestradeConnection> connections = QuestradeConnection.listAllConnections();

        List<ConnectionPortfolioDto> connectionResults = new ArrayList<>();
        for (QuestradeConnection connection : connections)
        {
            connectionResults.add(buildConnectionPortfolio(connection));
        }

        return aggregate(connectionResults);
    }

    private ConnectionPortfolioDto buildConnectionPortfolio(QuestradeConnection connection)
    {
        AccountsResponse accountsResponse = withRetry(connection,
                (client, bearer) -> client.getAccounts(bearer));

        ConnectionPortfolioDto dto = new ConnectionPortfolioDto();
        dto.label = connection.label;
        dto.accounts = new ArrayList<>();

        if (accountsResponse.accounts != null)
        {
            for (AccountDto account : accountsResponse.accounts)
            {
                dto.accounts.add(buildAccountBalance(connection, account));
            }
        }
        return dto;
    }

    private AccountCorrectedBalanceDto buildAccountBalance(QuestradeConnection connection, AccountDto account)
    {
        BalancesResponse balances = withRetry(connection,
                (client, bearer) -> client.getBalances(bearer, account.number));
        PositionsResponse positions = withRetry(connection,
                (client, bearer) -> client.getPositions(bearer, account.number));

        List<PositionDto> posList = positions.positions != null ? positions.positions : List.of();

        Map<Long, String> securityTypeBySymbolId = lookupSecurityTypes(connection, posList);

        BigDecimal optionsMarketValue = BigDecimal.ZERO;
        BigDecimal optionsOpenPnl = BigDecimal.ZERO;
        BigDecimal nonOptionMarketValue = BigDecimal.ZERO;
        BigDecimal nonOptionOpenPnl = BigDecimal.ZERO;
        BigDecimal nonOptionCostBasis = BigDecimal.ZERO;

        for (PositionDto position : posList)
        {
            boolean isOption = OPTION_SECURITY_TYPE.equalsIgnoreCase(securityTypeBySymbolId.get(position.symbolId));
            position.isOption = isOption;

            if (isOption)
            {
                optionsMarketValue = optionsMarketValue.add(nz(position.currentMarketValue));
                optionsOpenPnl = optionsOpenPnl.add(nz(position.openPnl));
            }
            else
            {
                nonOptionMarketValue = nonOptionMarketValue.add(nz(position.currentMarketValue));
                nonOptionOpenPnl = nonOptionOpenPnl.add(nz(position.openPnl));
                nonOptionCostBasis = nonOptionCostBasis.add(nz(position.totalCost));
            }
        }

        BalanceDto combinedCad = firstCad(balances.combinedBalances);
        BigDecimal cash = combinedCad != null ? nz(combinedCad.cash) : BigDecimal.ZERO;
        BigDecimal rawTotalEquity = combinedCad != null ? nz(combinedCad.totalEquity) : BigDecimal.ZERO;
        BigDecimal rawMarketValue = combinedCad != null ? nz(combinedCad.marketValue) : BigDecimal.ZERO;

        BigDecimal correctedEquity = cash.add(nonOptionMarketValue);

        AccountCorrectedBalanceDto dto = new AccountCorrectedBalanceDto();
        dto.accountNumber = account.number;
        dto.accountType = account.type;
        dto.rawMarketValue = rawMarketValue;
        dto.rawTotalEquity = rawTotalEquity;
        dto.optionsMarketValue = optionsMarketValue;
        dto.optionsOpenPnl = optionsOpenPnl;
        dto.cash = cash;
        dto.correctedMarketValue = nonOptionMarketValue;
        dto.correctedEquity = correctedEquity;
        dto.correctedOpenPnl = nonOptionOpenPnl;
        dto.correctedCostBasis = nonOptionCostBasis;
        dto.correctedPercentPnl = percentPnl(nonOptionOpenPnl, nonOptionCostBasis);
        dto.positionLines = posList;
        return dto;
    }

    private Map<Long, String> lookupSecurityTypes(QuestradeConnection connection, List<PositionDto> positions)
    {
        Map<Long, String> result = new HashMap<>();
        if (positions.isEmpty())
        {
            return result;
        }
        String ids = positions.stream()
                .map(p -> String.valueOf(p.symbolId))
                .distinct()
                .collect(Collectors.joining(","));

        SymbolsResponse symbolsResponse = withRetry(connection,
                (client, bearer) -> client.getSymbols(bearer, ids));

        if (symbolsResponse.symbols != null)
        {
            for (SymbolDto symbol : symbolsResponse.symbols)
            {
                result.put(symbol.symbolId, symbol.securityType);
            }
        }
        return result;
    }

    /**
     * Runs a Questrade API call using the connection's currently-cached access token.
     * If Questrade rejects it with 401 (token invalid despite our own expiry bookkeeping
     * saying it should still be good - e.g. real lifetime shorter than expected, clock
     * drift, or the token was invalidated on Questrade's side), forces a fresh refresh
     * and retries exactly once. If the refresh token itself is dead, this surfaces the
     * clear "generate a new one from App Hub" error from QuestradeTokenService instead of
     * a confusing 401.
     */
    private <T> T withRetry(QuestradeConnection connection, BiFunction<QuestradeApiClient, String, T> apiCall)
    {
        QuestradeApiClient client = tokenService.apiClientFor(connection);
        String bearer = "Bearer " + tokenService.getValidAccessToken(connection);
        try
        {
            return apiCall.apply(client, bearer);
        } catch (WebApplicationException e)
        {
            if (e.getResponse() != null && e.getResponse().getStatus() == 401)
            {
                tokenService.refresh(connection); // force, ignoring cached expiry
                QuestradeApiClient retryClient = tokenService.apiClientFor(connection);
                String retryBearer = "Bearer " + connection.accessToken;
                return apiCall.apply(retryClient, retryBearer);
            }
            throw e;
        }
    }

    private PortfolioSummaryDto aggregate(List<ConnectionPortfolioDto> connectionResults)
    {
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalOptionsMarketValue = BigDecimal.ZERO;
        BigDecimal totalCorrectedMarketValue = BigDecimal.ZERO;
        BigDecimal totalCorrectedEquity = BigDecimal.ZERO;
        BigDecimal totalCorrectedOpenPnl = BigDecimal.ZERO;
        BigDecimal totalCorrectedCostBasis = BigDecimal.ZERO;

        for (ConnectionPortfolioDto connection : connectionResults)
        {
            for (AccountCorrectedBalanceDto account : connection.accounts)
            {
                totalCash = totalCash.add(account.cash);
                totalOptionsMarketValue = totalOptionsMarketValue.add(account.optionsMarketValue);
                totalCorrectedMarketValue = totalCorrectedMarketValue.add(account.correctedMarketValue);
                totalCorrectedEquity = totalCorrectedEquity.add(account.correctedEquity);
                totalCorrectedOpenPnl = totalCorrectedOpenPnl.add(account.correctedOpenPnl);
                totalCorrectedCostBasis = totalCorrectedCostBasis.add(account.correctedCostBasis);
            }
        }

        PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.connections = connectionResults;
        summary.totalCash = totalCash;
        summary.totalOptionsMarketValue = totalOptionsMarketValue;
        summary.totalCorrectedMarketValue = totalCorrectedMarketValue;
        summary.totalCorrectedEquity = totalCorrectedEquity;
        summary.totalCorrectedOpenPnl = totalCorrectedOpenPnl;
        summary.totalCorrectedPercentPnl = percentPnl(totalCorrectedOpenPnl, totalCorrectedCostBasis);
        return summary;
    }

    private BigDecimal percentPnl(BigDecimal openPnl, BigDecimal costBasis)
    {
        if (costBasis.compareTo(BigDecimal.ZERO) == 0)
        {
            return BigDecimal.ZERO;
        }
        return openPnl.divide(costBasis, 6, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private BalanceDto firstCad(List<BalanceDto> balances)
    {
        if (balances == null)
        {
            return null;
        }
        return balances.stream().filter(b -> "CAD".equals(b.currency)).findFirst().orElse(null);
    }

    private BigDecimal nz(BigDecimal value)
    {
        return value != null ? value : BigDecimal.ZERO;
    }

}