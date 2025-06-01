# Concurrent Entity Change Logger (No External Dependencies)

> **멀티스레드 환경에서 JPA 엔티티 변경 사항을 감지하고, 외부 인프라 없이 안전하게 비동기 저장하는 구조**  
> Logging without Kafka / Redis / Elastic. Built with pure Java & Spring.

---

## Project Structure

```
src/
├── main/java/com/pandaterry/concurrent_entity_change_logger/
│ ├── core/ # 핵심 기능 구현
│ │ ├── entity/ # 엔티티 클래스
│ │ ├── enumerated/ # 열거형
│ │ ├── factory/ # 팩토리 클래스
│ │ ├── repository/ # 리포지토리
│ │ ├── strategy/ # 로깅 전략
│ │ ├── tracker/ # 엔티티 변경 추적
│ │ └── util/ # 유틸리티
│ └── monitoring/ # prometheus 모니터링
│ └── loadtest/ # 부하테스트용 API
│
└── test/java/com/pandaterry/concurrent_entity_change_logger/
└── core/ # 테스트 코드
```

## 주요 컴포넌트

- `core`: 핵심 엔티티 변화 로깅 기능 구현
- `monitoring`: 로깅 메트릭 수집 및 모니터링
- `loadtest`: 부하테스트용 API 엔드포인트 및 엔티티 등

## Why Was This Built?

> 기존에도 Entity 변경 사항을 추적할 수 있는 다양한 방법들이 존재합니다.
예: Spring Data Auditing, AOP 기반 로깅, Kafka 기반 Change Data Capture, ELK 등.

그러나 본 시스템은 다음과 같은 **제약 조건**과 **현실적 요구사항**에서 출발했습니다:

1. **외부 인프라 사용 불가**
   - 폐쇄망 환경 (예: 금융기관 내부망, 군/국가기관)
   - Kafka, Redis, ELK 등 미지원

2. **고신뢰 변경 로그 필요**
   - 운영 중 장애 분석 및 규제 대응 목적의 변경 이력
   - 최소 단위 변경까지 추적 가능한 JSON diff

3. **비동기 + 고성능 처리**
   - TPS 5,000 이상 발생 가능 (예: 배치 업로드, 실시간 이벤트)

4. **장애 대비 운영성 확보**
   - flush 보장, retry queue, shutdown hook 포함

이 구조는 ‘모든 상황에서 가장 우아한 솔루션’은 아닙니다.  
하지만 위와 같은 조건 속에서는 **가장 신뢰성 있고 실용적인 구조**입니다.

## Sequence Diagram

![Image](https://github.com/user-attachments/assets/6550d18a-c620-472d-bfe4-8bba36021758)

## Features

- Hibernate Entity 변경 사항 감지 (Insert / Update / Delete)
- 멀티스레드 환경에서 안정적인 처리 (`BlockingQueue + ThreadPoolExecutor`)
- `JDBC batch insert` 기반 빠른 로그 저장 (JPA `saveAll()` 미사용)
- JSON diff 기반 필드 변경 감지 (optional)
- 운영 종료 시 flush 보장 (`shutdown hook`)

---

## Tradeoffs & 현실적 한계

“서버 메모리에 로그를 올리고, GC 터지면 날아가는 거 아니냐?”라는 의문은 타당합니다.  
이 구조는 아래와 같은 **명확한 트레이드오프와 대응 전략**을 기반으로 설계되었습니다:

| 항목 | 대응 방식 |
|------|-----------|
| **메모리 기반 큐의 휘발성** | 일정 주기 flush + shutdown hook + 향후 파일 fallback 구조 예정 |
| **GC Pressure** | 정적 객체 재사용(RingBuffer 유사 구조 고려), `batchInsert`로 메모리 스파이크 최소화 |
| **서버 다운 시 유실 위험** | 운영 환경에서 flush 간격, queue size, graceful shutdown 정책 조정 |
| **로그 유실 검증** | 강제 장애 및 GC 유도 시나리오 테스트로 flush coverage 확보 검증 완료 |
| **모니터링 한계** | Prometheus 기반 메트릭 및 `/metrics/logging` API 도입 예정 |

---

## Goals

> 이 프로젝트는 단순한 기술 시연을 넘어,  
**운영 환경에서도 적용 가능한 '순수 Java 기반 고신뢰 로깅 시스템' 구축**을 목표로 합니다.

- 외부 인프라가 없는 환경에서도 초당 10,000건 이상의 변경 로그 처리 가능
- 실패 시 재시도 및 flush 보장으로 신뢰성 확보
- 필드 단위 변경 사항(JSON diff)까지 명확히 추적
- 추후 Web 기반 로그 뷰어까지 제공하여 운영 편의성 확보
- 선택적으로 OpenSearch 연동하여 hybrid 구조 확장 가능

## Architecture Overview

```
[Hibernate EventListener]

▼

[EntityChangeListener]

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

1. Hibernate 이벤트(PostInsert, PostUpdate, PostDelete)를 가로채서 엔티티 변경사항 감지

2. 엔티티명, ID, 변경 필드, 요청자 등 메타데이터로 LogEntry 생성

3. 내부 BlockingQueue에 enqueue

4. ThreadPoolExecutor가 백그라운드에서 로그를 배치로 소비

5. JdbcTemplate.batchUpdate()로 빠르게 저장

6. 일정 주기마다 flush (@Scheduled) + 종료 시 shutdown hook 등록

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
- X => Kafka 없음

- X => Redis 없음

- X => Elastic 없음

- O => 순수 Spring + JDBC 기반


## Future Improvements
- [ ] JSON diff 자동 생성기 (Map<String, [old, new]>)

- [ ] SQL fallback (local file queue on DB failure)

- [ ] 시각화 대시보드용 REST API (/metrics/logging)

- [ ] 로그 뷰어 (Web 기반 간단 UI)

## Roadmap: Towards Lock-Free, Wait-Free Logging

현재 구조는 `BlockingQueue` 기반으로 안정성을 확보하고 있으나,  
향후에는 LMAX Disruptor처럼 **GC 최소화 + lock-free 처리 구조**로 발전시킬 계획입니다.

- **사용 중인 로그 항목만 메모리에 올리는 RingBuffer 구조**
- **CAS 기반 lock-free, wait-free 이벤트 처리**
- **객체 재사용을 통한 GC 압력 최소화**
- **TPS 50,000+, P99 latency 10ms 미만 목표**

> Kafka, Redis 없이도 고성능 구조를 실현하기 위한  
> **순수 Java 기반의 lightweight Disruptor 대안**을 지향합니다.