# Stat Aggregation Service

A high-performance, event-driven microservice built with **Java 21** and **Spring Boot 3** that aggregates real-time player statistics and manages live leaderboards. Originally ported from a Go implementation, this service demonstrates polyglot persistence, distributed locking, and event streaming principles suitable for high-throughput gaming environments.

---

## 1. Context & Background

In competitive online gaming, handling match results requires balancing real-time speed with data integrity:
* **High Write Throughput:** Thousands of matches finish simultaneously, generating heavy write bursts.
* **Idempotency Requirements:** Network retries or duplicate payload submissions must not artificially inflate player stats.
* **Low Latency Leaderboards:** Querying top-tier players directly from relational databases using `ORDER BY` creates severe DB bottlenecks.

This project addresses these challenges by combining **Oracle DB 23ai** (ACID storage), **Redis** (caching, fast leaderboard sorting, idempotency lock), and **Apache Kafka** (decoupled event streaming).

---

## 2. System Architecture

```text
                     +---------------------------+
                     |    Client / API Caller    |
                     +---------------------------+
                                   |
                         HTTP REST Requests
                                   v
             +-----------------------------------------------+
             |       Spring Boot Service App (:8080)        |
             |  (StatController / StatAggregationService)    |
             +-----------------------------------------------+
                 /                 |                 \
     1. Check/Set Lock      2. Read/Write Stats     3. Publish Event
               /                   |                   \
              v                    v                    v
      +--------------+    +------------------+    +-------------------+
      |  Redis ZSet  |    | Oracle DB 23ai   |    |    Apache Kafka   |
      |   & Cache    |    |  (ACID Storage)  |    | (KRaft Event-Bus) |
      +--------------+    +------------------+    +-------------------+
      • Idempotency       • Permanent state       • Real-time match
        Lock (SET NX)       of player stats         event streaming
      • Fast Leaderboards • Relational integrity  • Decoupled event
        Sorted Sets         and aggregation         consumers

```

### Component Roles

* **Apache Kafka (Event Streaming):** Acts as an asynchronous event bus. When a match finishes, the service publishes a JSON event to the `match-events` topic. This decouples match processing from downstream consumers (e.g., analytics, achievements, rewards) and ensures fault tolerance.


* **Redis (In-Memory Speed & Locking):**
* **Distributed Locking (`SET NX`):** Prevents duplicate match processing by acquiring a temporary lock on `match_id`.


* **Real-time Leaderboard:** Uses **Redis Sorted Sets (ZSET)** to maintain real-time *O(log N)* rank sorting by kills/wins, avoiding expensive database `ORDER BY` queries.




* **Oracle DB 23ai (System of Record):** Provides persistent, ACID-compliant storage for historical career statistics (`PLAYER_STATS` table).



---

## 3. Project Structure

```text
statistic-aggregation
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── stataggregation
│   │   │               ├── StatAggregationApplication.java
│   │   │               ├── config
│   │   │               │   └── KafkaConfig.java
│   │   │               ├── controller
│   │   │               │   └── StatController.java
│   │   │               ├── model
│   │   │               │   ├── MatchRequest.java
│   │   │               │   ├── PlayerMatchStat.java
│   │   │               │   └── PlayerStatsResponse.java
│   │   │               ├── repository
│   │   │               │   ├── PlayerStatEntity.java
│   │   │               │   └── PlayerStatRepository.java
│   │   │               └── service
│   │   │                   └── StatAggregationService.java
│   │   └── resources
│   │       └── application.properties
│   └── test
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── prometheus.yml
└── README.md

```

---

## 4. How to Run Step-by-Step

### Prerequisites

* **Docker** & **Docker Compose** installed
* **Java 21** & **Maven 3.x** (optional, if building locally outside Docker)

### Steps

1. **Clone the repository:**
```bash
git clone https://github.com/rezzamaqfiro/statistic-aggregation.git
cd statistic-aggregation

```


2. **Start the infrastructure and application using Docker Compose:**
```bash
docker compose up --build -d

```


3. **Verify running services:**
```bash
docker compose ps

```


4. **Flush Redis cache during local testing (if retrying previously submitted match IDs):**
```bash
docker compose exec redis redis-cli FLUSHALL

```



---

## 5. Included UI Dashboards

The project container stack includes multiple web dashboards for monitoring state across services:

| Dashboard | Access URL | Credentials / Notes |
| --- | --- | --- |
| **Kafka UI** | [http://localhost:8081](http://localhost:8081) | Inspect topics (`match-events`), offsets, and live published payloads. | 
| **CloudBeaver (Oracle DB)** | [http://localhost:8082](http://localhost:8082) | Admin DB client. Connect to Host: `oracle-db`, Port: `1521`, Service Name: `FREEPDB1`, User: `system`, Pass: `oracle_pass`.
| **Prometheus UI** | [http://localhost:9090](http://localhost:9090) | Query metric graphs (e.g., `http_server_requests_seconds_count`). |
| **Spring Boot Actuator** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Raw application metrics exporter endpoint.|

---

## 6. API Verification Commands (`curl`)

Run these commands in order to test all core functional requirements:

### 1. Health Check

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8080/health

```

* **Expected Output:** `{"status":"ok"}` with `HTTP 200`


### 2. Submit Match 1 (Case 1)

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/matches \
  -H "Content-Type: application/json" \
  -d '{"match_id":"match-1","duration_seconds":1800,"players":[
    {"player_id":"player-A","kills":10,"won":true},
    {"player_id":"player-B","kills":4,"won":false}]}'

```

* **Expected Output:** `{"status":"success"}` with `HTTP 200`


### 3. Submit Match 2 (Case 2)

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/matches \
  -H "Content-Type: application/json" \
  -d '{"match_id":"match-2","duration_seconds":2400,"players":[
    {"player_id":"player-A","kills":6,"won":false},
    {"player_id":"player-C","kills":15,"won":true}]}'

```

* **Expected Output:** `{"status":"success"}` with `HTTP 200`


### 4. Get Player Career Stats (Case 3)

```bash
curl -s -X POST http://localhost:8080/players/stats \
  -H "Content-Type: application/json" \
  -d '{"player_ids":["player-A","player-B","player-C"]}'

```

* **Expected Output:**
```json
[
  {"player_id":"player-A","kills":16,"wins":1,"playtime_seconds":4200},
  {"player_id":"player-B","kills":4,"wins":0,"playtime_seconds":1800},
  {"player_id":"player-C","kills":15,"wins":1,"playtime_seconds":2400}
]

```



### 5. Leaderboard by Kills (Top 2) (Case 4)

```bash
curl -s "http://localhost:8080/leaderboard?stat=kills&limit=2"

```

* **Expected Output:**
```json
[
  {"rank":1,"player_id":"player-A","score":16},
  {"rank":2,"player_id":"player-C","score":15}
]

```


### 6. Duplicate Match Submission / Idempotency Test (Case 5)

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/matches \
  -H "Content-Type: application/json" \
  -d '{"match_id":"match-1","duration_seconds":1800,"players":[
    {"player_id":"player-A","kills":10,"won":true}]}'

```

* **Expected Output:** `{"error":"match already processed"}` with `HTTP 409`


### 7. Non-existent Player Handling (Case 6)

```bash
curl -s -X POST http://localhost:8080/players/stats \
  -H "Content-Type: application/json" \
  -d '{"player_ids":["player-A","player-unknown"]}'

```

* **Expected Output:** `HTTP 200` containing stats for `player-A` (`player-unknown` is omitted)



### 8. Leaderboard by Wins (Case 7)

```bash
curl -s "http://localhost:8080/leaderboard?stat=wins&limit=2"

```

### 9. Prometheus Metrics Endpoint

```bash
curl -s http://localhost:8080//actuator/prometheus

```
---

&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp; Built with ❤️ by Maqfiro.
