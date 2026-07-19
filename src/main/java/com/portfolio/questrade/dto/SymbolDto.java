package com.portfolio.questrade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolDto {
    public long symbolId;
    public String symbol;
    public String securityType; // "Stock", "Option", "Right", "ETF", "Index", "MutualFund", "Bond", ...
}
