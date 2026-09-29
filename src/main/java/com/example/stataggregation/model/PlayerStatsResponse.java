package com.example.stataggregation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerStatsResponse (
    @JsonProperty("player_id") String playerId,
    long kills,
    long wins,
    @JsonProperty("playtime_seconds") long playtimeSeconds
) {}