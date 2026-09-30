package com.example.stataggregation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;


import com.example.stataggregation.model.MatchRequest;
import com.example.stataggregation.model.PlayerStatsResponse;
import com.example.stataggregation.repository.PlayerStatEntity;
import com.example.stataggregation.model.PlayerMatchStat;
import com.example.stataggregation.repository.PlayerStatRepository;

@Service
public class StatAggregationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PlayerStatRepository playerStatRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public boolean processMatch(MatchRequest request) {
        String lockKey = "match:processed" + request.matchId();

        // Idempotency Check
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofDays(7));
        if (Boolean.FALSE.equals(isNew)) {
            return false;
        }

        // Update Redis 
        for (PlayerMatchStat p : request.players()) {
            String playerKey = "player:" + p.playerId() + ":stats";

            redisTemplate.opsForHash().increment(playerKey, "kills", p.kills());
            redisTemplate.opsForHash().increment(playerKey, "wins", p.won() ? 1 : 0);
            redisTemplate.opsForHash().increment(playerKey, "playtime", request.durationSeconds());

            // Sort  Leaderboards
            redisTemplate.opsForZSet().incrementScore("leaderboard:kills", p.playerId(), p.kills());
            redisTemplate.opsForZSet().incrementScore("leaderboard:wins", p.playerId(), p.won() ? 1 : 0);

            // save to DB
            PlayerStatEntity stat = playerStatRepository.findById(p.playerId())
                .orElse(new PlayerStatEntity(p.playerId(), 0, 0, 0));
            
            stat.setKills(stat.getKills() + p.kills());
            stat.setWins(stat.getWins() + (p.won() ? 1 : 0));
            stat.setPlaytimeSeconds(stat.getPlaytimeSeconds() + request.durationSeconds());
            
            playerStatRepository.save(stat);
        }

        // Publish Event 
        kafkaTemplate.send("match-events", request.matchId(), request);

        return true;
    }

    public List<PlayerStatsResponse> getPlayerStats(List<String> playerIds) {
        return null;
    }

    public List<Map<String, Object>> getLeaderboard(String stat, int limit) {
        return null;
    }
}
