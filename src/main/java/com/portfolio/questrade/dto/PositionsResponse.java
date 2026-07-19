package com.portfolio.questrade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionsResponse {
    public List<PositionDto> positions;
}
