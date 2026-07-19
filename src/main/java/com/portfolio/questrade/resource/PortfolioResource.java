package com.portfolio.questrade.resource;

import com.portfolio.questrade.dto.response.PortfolioSummaryDto;
import com.portfolio.questrade.service.PortfolioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/portfolio")
@Produces(MediaType.APPLICATION_JSON)
public class PortfolioResource {

    @Inject
    PortfolioService portfolioService;

    @GET
    public PortfolioSummaryDto getPortfolio() {
        return portfolioService.getCorrectedPortfolio();
    }
}
