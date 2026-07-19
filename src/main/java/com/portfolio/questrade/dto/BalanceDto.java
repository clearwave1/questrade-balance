package com.portfolio.questrade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    public String currency;
    public BigDecimal cash;
    public BigDecimal marketValue;   // Questrade's raw market value - includes options
    public BigDecimal totalEquity;   // Questrade's raw equity - includes options
    public BigDecimal buyingPower;
    public BigDecimal maintenanceExcess;
    public boolean isRealTime;
}
