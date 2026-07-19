package com.portfolio.questrade.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioSummaryDto {
    public List<ConnectionPortfolioDto> connections;

    // Grand totals across every account, every connection
    public BigDecimal totalCash;
    public BigDecimal totalOptionsMarketValue;
    public BigDecimal totalCorrectedMarketValue;
    public BigDecimal totalCorrectedEquity;
    public BigDecimal totalCorrectedOpenPnl;
    public BigDecimal totalCorrectedPercentPnl;
}
