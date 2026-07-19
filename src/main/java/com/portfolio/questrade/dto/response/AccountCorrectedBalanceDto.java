package com.portfolio.questrade.dto.response;

import com.portfolio.questrade.dto.PositionDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccountCorrectedBalanceDto {
    public String accountNumber;
    public String accountType;

    // What Questrade Edge shows you (includes options)
    public BigDecimal rawMarketValue;
    public BigDecimal rawTotalEquity;

    // What's actually sitting in options right now
    public BigDecimal optionsMarketValue;
    public BigDecimal optionsOpenPnl;

    // Corrected numbers with options stripped out
    public BigDecimal cash;
    public BigDecimal correctedMarketValue;
    public BigDecimal correctedEquity;
    public BigDecimal correctedOpenPnl;
    public BigDecimal correctedCostBasis; // sum of totalCost for non-option positions, used for weighted aggregation
    public BigDecimal correctedPercentPnl;
    public List<PositionDto> positionLines = new ArrayList<PositionDto>();
}
