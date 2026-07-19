package com.portfolio.questrade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDto {
    public String type;          // e.g. Margin, TFSA, RRSP
    public String number;        // account number, used as :id in other endpoints
    public String status;
    public boolean isPrimary;
    public boolean isBilling;
    public String clientAccountType;
}
