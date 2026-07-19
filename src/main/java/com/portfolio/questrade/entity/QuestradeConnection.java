package com.portfolio.questrade.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.Instant;
import java.util.List;

/**
 * One Questrade login. Authorized-trader access (e.g. a spouse's account you can view/trade)
 * lives under THAT person's login, so each person you have API access for needs their own
 * connection with their own refresh token generated from their Questrade App Hub.
 */
@Entity
public class QuestradeConnection extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String label; // e.g. "don", "wife"

    // Questrade refresh tokens are SINGLE USE - every successful refresh returns a brand new
    // one that must overwrite this field immediately, or the connection is dead until you
    // manually generate a new token from App Hub.
    // Plain "text" column, not @Lob - on Postgres @Lob maps String to a large object (OID),
    // which requires non-autocommit access and breaks plain GET reads. These tokens are short
    // strings; they don't need CLOB semantics at all.
    @Column(nullable = false, columnDefinition = "text")
    public String refreshToken;

    @Column(columnDefinition = "text")
    public String accessToken;

    public String apiServer; // e.g. https://api01.iq.questrade.com/ - changes per login, given at refresh time

    public Instant tokenExpiresAt;

    public Instant lastRefreshedAt;

    public static QuestradeConnection findByLabel(String label) {
        return find("label", label).firstResult();
    }

    public static List<QuestradeConnection> listAllConnections() {
        return listAll();
    }
}