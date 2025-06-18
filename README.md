# Concurrent Entity Change Logger (No External Dependencies)

> **멀티스레드 환경에서 JPA 엔티티 변경 사항을 감지하고, 외부 인프라 없이 안전하게 비동기 저장하는 구조**  
> Logging without Kafka / Redis / Elastic. Built with pure Java & Spring.

---

## Project Structure

```
.
├── core                # 엔티티 변경 감지부터 로그 생성/저장까지의 핵심 로직
│   ├── application     # 로깅 전략(strategy) 및 트래커(tracker) 구현
│   │   ├── strategy    # 로깅 전략 인터페이스 및 구현체
│   │   └── tracker     # Insert/Delete/Update 등 이벤트별 트래커
│   ├── common          # 공통 애노테이션 및 유틸리티
│   │   ├── annotation  # 로그 제외용 애노테이션
│   │   └── util        # 엔티티 상태 복제 등 유틸 클래스
│   ├── domain          # LogEntry, Operation 등 도메인 모델
│   └── infrastructure  # 설정, 리스너, 리포지토리, 저장소 구현
│       ├── config      # 프로퍼티·전략·리스너·JSON 설정
│       ├── listener    # Hibernate 이벤트 리스너
│       ├── persistence # JPA 리포지토리 및 팩토리
│       └── storage     # 디스크 쓰기 및 체크포인트 관리
├── loadtest            # k6 부하 테스트용 API
│   ├── controller      # 테스트용 REST 컨트롤러
│   ├── dto             # 요청/응답 DTO
│   ├── entity          # 테스트용 엔티티 정의
│   ├── repository      # 테스트용 리포지토리
│   └── service         # 테스트 시나리오용 서비스
└── monitoring          # 메트릭 수집 및 기록
    ├── config          # Micrometer 설정
    ├── constants       # 메트릭 이름 상수
    └── service         # 로그 처리 메트릭 기록기

```

## Why Was This Built?

> 기존에도 Entity 변경 사항을 추적할 수 있는 다양한 방법들이 존재합니다.
예: Spring Data Auditing, AOP 기반 로깅, Kafka 기반 Change Data Capture, ELK 등.

그러나 본 시스템은 다음과 같은 **제약 조건**과 **현실적 요구사항**에서 출발했습니다:

1. **외부 인프라 사용 불가**
   - 폐쇄망 환경 (예: 금융기관 내부망, 군/국가기관)
   - Kafka, Redis, ELK 등 미지원

2. **고신뢰 변경 로그 필요**
   - 운영 중 장애 분석 및 규제 대응 목적의 변경 이력

3. **비동기 + 고성능 처리**
   - TPS 500~1000 발생 가능 (예: 배치 업로드, 실시간 이벤트)

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

# Test Results

## 부하 테스트 환경

- **도구**: k6  
- **기간**: 총 20분  
- **시나리오**: 세 가지 엔티티에 대해 Insert/Update 동시 실행  
- **로그 생성**: 26만 건 이상, 실패 없이 모두 생성  
- **지표 수집**: Prometheus + Grafana  

## 부하 테스트 결과

- **총 요청 수**: 274,728회  
- **테스트 시간**: 약 2분 (Iteration_rate ≈ 13.1/초)  
- **가상 사용자(VUs)**: 최대 38명 (설정 상 최대 100명)  
- **수신 데이터**: 73.4 MB → 초당 ≈ 84 KB/s  
- **전송 데이터**: 49.3 MB → 초당 ≈ 56 KB/s  
- **평균 응답 지연**: 4.15 ms  
- **중앙값(median)**: 2.91 ms  
- **p(90)**: 5.12 ms  
- **p(95)**: 8.23 ms  
- **최대 지연**: 179.26 ms  

## JVM 메모리 사용 비교
![Image](https://github.com/user-attachments/assets/d5aab13c-9da2-4b5c-b6a2-f951931cc041)
| 구분               | G1 Eden Peak | G1 Old Gen |
| ------------------ | ------------ | ---------- |
| 단건 Insert        | 300 MB       | 40 ~ 50 MB |
| JDBC Batch Insert  | 30 MB        | 40 ~ 50 MB |

- 배치 Insert 적용 전 평균 힙 사용: 180 MB  
- 적용 후 평균 힙 사용: 70 MB (약 2.5배 효율화)  

## 안정성 및 스루풋

#### 로그 유실 건수
![Image](https://github.com/user-attachments/assets/f5bdb9f2-df6f-4b94-b22c-e59f4ba371b8)

#### 처리량
![Image](https://github.com/user-attachments/assets/90cda428-50c1-44c9-890b-d80c46154f32)

- **유실 로그(Drop)**: 0건  
- **평균 처리량**: 초당 400 ~ 500건  
- **최대 처리량**: 초당 655건  
- **오류 발생 빈도**: 0건  

## 큐 동작 및 지연 시간

#### Queue 크기변화
![Image](https://github.com/user-attachments/assets/16ea0fd0-62fe-4a9b-a211-f1c973e898d1)

#### 지연시간
![Image](https://github.com/user-attachments/assets/9142e2f6-29a2-45ed-b50f-879ab1dd9991)

- **배치 사이즈**: 1,000건  
- **큐 최대 사이즈**: 100,000건  
- **큐 길이**: 배치 처리 전 최대 1,000 → 처리 후 초기화 반복  
- **로그 큐 입력 지연**: 초기에는 높으나 곧 안정화  
- **배치 Insert 지연**: 초기 설정 시간 소모 후 0.04 ~ 0.045 초 유지  


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

## Configuration Highlights
- 로그 비활성화 대상: @ExcludeFromLogging 어노테이션 또는 설정 기반

- 배치 크기: 100개 이상일 때 flush

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