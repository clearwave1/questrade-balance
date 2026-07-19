package com.portfolio.questrade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionDto {
    public String symbol;
    public long symbolId;
    public BigDecimal openQuantity;
    public BigDecimal currentMarketValue;
    public BigDecimal currentPrice;
    public BigDecimal averageEntryPrice;
    public BigDecimal closedPnl;
    public BigDecimal openPnl;
    public BigDecimal totalCost;
    public boolean isRealTime;
    public boolean isUnderReorg;
    public boolean isOption;
}
