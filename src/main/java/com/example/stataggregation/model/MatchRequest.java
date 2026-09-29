package com.example.stataggregation.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MatchRequest (
    @JsonProperty("match_id") String matchId,
    @JsonProperty ("duration_seconds") long durationSeconds,
    List<PlayerMatchStat> players
) {}
