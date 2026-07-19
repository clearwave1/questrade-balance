package com.portfolio.questrade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancesResponse {
    public List<BalanceDto> perCurrencyBalances;
    public List<BalanceDto> combinedBalances;
    public List<BalanceDto> sodPerCurrencyBalances;
    public List<BalanceDto> sodCombinedBalances;
}
