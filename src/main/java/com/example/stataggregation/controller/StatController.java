package com.example.stataggregation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.stataggregation.service.StatAggregationService;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.stataggregation.model.PlayerStatsResponse;
import com.example.stataggregation.model.MatchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController 
public class StatController {

    @Autowired
    private StatAggregationService service;

    @PostMapping("/matches")
    public ResponseEntity<?> submitMatch(@RequestBody MatchRequest request) {
        boolean processed = service.processMatch(request);
        if (!processed) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Match already processed"));
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/players/stats")
    public ResponseEntity<List<PlayerStatsResponse>> getStats(@RequestBody Map<String, List<String>> body) {
        List<String> playerIds = body.get("player_ids");
        return ResponseEntity.ok(service.getPlayerStats(playerIds));
    }

    @GetMapping ("/leaderboard")
    public ResponseEntity<?> getLeaderboard(
            @RequestParam(defaultValue = "kills") String stat,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.getLeaderboard(stat, limit));
    }

    @GetMapping ("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

}