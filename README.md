# Thread-Safe Entity Change Logger (No External Dependencies)

> **멀티스레드 환경에서 JPA 엔티티 변경 사항을 감지하고, 외부 인프라 없이 안전하게 비동기 저장하는 구조**  
> Logging without Kafka / Redis / Elastic. Built with pure Java & Spring.

---

## Features

- ✅ Hibernate Entity 변경 사항 감지 (Insert / Update / Delete)
- ✅ 멀티스레드 환경에서 안정적인 처리 (`BlockingQueue + ThreadPoolExecutor`)
- ✅ 외부 인프라(Kafka, Redis 등) 전혀 없이도 고TPS 대응
- ✅ `JDBC batch insert` 기반 빠른 로그 저장 (JPA `saveAll()` 미사용)
- ✅ `requestId`, `actorId` 기반 운영 추적성 확보
- ✅ JSON diff 기반 필드 변경 감지 (optional)
- ✅ 운영 종료 시 flush 보장 (`shutdown hook`)

---

## Architecture Overview

```
[Hibernate EventListener]

▼

[CustomEntityChangeListener]

▼

[Concurrent Log Queue] ← ArrayBlockingQueue (Thread-safe)

▼

[ThreadPoolExecutor Workers]

▼

[LogEntryJdbcWriter]

▼

[Bulk INSERT into DB using JdbcTemplate]
```

##  How It Works
Hibernate 이벤트(PostInsert, PostUpdate, PostDelete)를 가로채서 엔티티 변경사항 감지

엔티티명, ID, 변경 필드, 요청자 등 메타데이터로 LogEntry 생성

내부 BlockingQueue에 enqueue

ThreadPoolExecutor가 백그라운드에서 로그를 배치로 소비

JdbcTemplate.batchUpdate()로 빠르게 저장

일정 주기마다 flush (@Scheduled) + 종료 시 shutdown hook 등록

##  Performance Expectations
| 구성                      | TPS 기준 (보수적)                |
| ----------------------- | --------------------------- |
| JPA `saveAll()` 방식      | 2,000 \~ 4,000 logs/sec     |
| JDBC `batchUpdate()` 방식 | 8,000 \~ 20,000 logs/sec    |
| JVM 단독 구조               | 최대 10,000 logs/sec 이상 대응 가능 |


> 단일 노드, 외부 의존성 없이도 고성능 유지 가능

## Query Examples

```sql
-- 특정 엔티티 변경 이력
SELECT * FROM log_entry
WHERE entity_name = 'Order' AND entity_id = '123'
ORDER BY created_at DESC;

-- 특정 요청(requestId)에서 발생한 모든 변경
SELECT * FROM log_entry
WHERE request_id = 'abc123-df22...';

-- 특정 필드가 변경된 로그 찾기 (JSON)
SELECT * FROM log_entry
WHERE JSON_EXTRACT(field_changes, '$.status') IS NOT NULL;
```


## Configuration Highlights
- 로그 비활성화 대상: @ExcludeFromLogging 어노테이션 또는 설정 기반

- 배치 크기: 100개 이상일 때 flush

- flush 실패 시: retryQueue에 재적재

- shutdown 시 flush 보장: Runtime.getRuntime().addShutdownHook()

##  No External Infra Required
- ❌ Kafka 없음

- ❌ Redis 없음

- ❌ Elastic 없음

- ✅ 순수 Spring + JDBC 기반


## Future Improvements
- [ ] JSON diff 자동 생성기 (Map<String, [old, new]>)

- [ ] SQL fallback (local file queue on DB failure)

- [ ] 시각화 대시보드용 REST API (/metrics/logging)

- [ ] 로그 뷰어 (Web 기반 간단 UI)