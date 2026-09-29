package com.example.stataggregation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerMatchStat (
    @JsonProperty ("player_id") String playerId,
    long kills,
    boolean won
) {}