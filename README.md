# Concurrent Entity Change Logger (No External Dependencies)

![Image](https://github.com/user-attachments/assets/1f9cdf3c-8396-430b-80c3-498fea615de3)

> **멀티스레드 환경에서 JPA 엔티티 변경 사항을 감지하고, 외부 인프라 없이 안전하게 비동기 저장하는 구조**  
> Logging without Kafka / Redis / Elastic. Built with pure Java & Spring.


## 목차
- [개발 배경](#background)
- [프로젝트 구성](#project-structure)
- [빠르게 시작하기](#quick-start)
- [내부 로직 상세](#internal-logic)
- [시퀀스 다이어그램](#sequence-diagram)
- [목표/로드맵](#goals)
- [부하 테스트 결과](#test-results)
---

## Background

> 기존에도 Entity 변경 사항을 추적할 수 있는 다양한 방법들이 존재합니다.
> 예: Spring Data Auditing, AOP 기반 로깅, Kafka 기반 Change Data Capture, ELK 등.

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

## Project Structure

```
entity_change_logging/          # `메인 도메인 - 엔티티 변경 로깅 시스템`
├── domain/                        #  >> 도메인 계층 - 비즈니스 핵심 로직
│   ├── model/LogEntry.java       # 로그 엔트리 Aggregate Root
│   ├── vo/                       # Value Objects (ChangeSet, FieldChange 등)
│   ├── service/tracker/          # 변경 추적기들 (Insert/Update/Delete)
│   └── error/                    # 도메인 예외들
├── application/                   # >> 애플리케이션 계층 - 유스케이스 조합
│   ├── service/                  # 배치 처리, 로깅 서비스
│   └── port/                     # 인터페이스 정의
│       ├── input/                # Primary Ports (외부에서 호출)
│       └── output/               # Secondary Ports (외부 의존성)
├── adapter/                      # >> 어댑터 계층 - 외부 세계와 연결
│   ├── in/                       # Inbound Adapters (외부 → 도메인)
│   │   ├── listener/             # JPA 엔티티 변경 리스너
│   │   ├── scheduler/            # 배치 처리 스케줄러  
│   │   └── monitoring/           # 메트릭스 수집
│   └── out/                      # Outbound Adapters (도메인 → 외부)
│       ├── persistence/          # 데이터베이스 저장
│       ├── queue/                # 큐 시스템 연동
│       └── storage/              # 디스크 백업 저장

mock_commerce/                  # 테스트용 전자상거래 도메인
├── user/                         # 사용자 관리 (로깅 대상)
├── order/                        # 주문 관리 (로깅 대상)  
└── product/                      # 상품 관리 (로깅 대상)

shared/                         # 공통 인프라스트럭처
├── config/                       # 스프링 설정들
├── error/                        # 공통 예외 처리
└── infrastructure/               # 공통 유틸리티
```

## Quick Start

#### 1. 의존성(gradle)

```gradle
 // 공통
 compileOnly 'org.projectlombok:lombok'
 annotationProcessor 'org.projectlombok:lombok'
 implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
 
 // 웹
 implementation 'org.springframework.boot:spring-boot-starter-web'
 
 // 데이터
 implementation 'org.springframework.boot:spring-boot-starter-data-jdbc'
 implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
 implementation 'org.postgresql:postgresql'
 
 // 테스트(선택)
 testImplementation 'org.springframework.boot:spring-boot-starter-test'
 testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
 
 // 메트릭(선택)
 implementation 'io.micrometer:micrometer-registry-prometheus'
 implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

#### 2. 어플리케이션 설정(yml)

```yml
...

# 엔티티 변경 로깅 설정
entity-logging:
  # 전역 로깅 활성화 여부
  enabled: true
  # 로깅에서 제외할 엔티티 목록
  excluded-entities:
    - LogEntry
  # 스토리지에 저장할 타입(disk | noop) <- 현재는 추가적으로 디스크 저장만 지원합니다. 
  storage-type: disk
  # 처리 방식(blocking | noop) <- 현재는 블로킹큐만 지원합니다.
  process-type: blocking
  # 로깅 전략 설정
  strategy:
    # 로그 큐 크기 (메모리 사용량과 성능의 균형)
    queue-size: 100000
    # 로그 처리 스레드 수 (동시 처리량)
    thread-pool-size: 5
    # 배치 플러시 주기 (밀리초)
    flush-interval: 5000
```

#### 3. K6 테스트 확인
```bash
# k6파일이 있는 곳으로 이동
cd /k6-test

# 부하테스트 실행(mock-commerce API를 실행하며 뒤에서 로그가 저장됩니다.)
k6 run --summary-export=summary.json load_test_v1.js
```

## Internal Logic

#### 실시간 변경 감지

```
JPA Entity 변경 → Hibernate Listener → EntityChangeTracker → Queue
```

#### 비동기 배치 처리

```
Queue → BatchProcessingService → Database 저장 + 디스크 백업
```

#### 성능 최적화

- [x] 논블로킹 큐: 메인 트랜잭션에 영향 없는 비동기 처리

- [x] 배치 INSERT: 대량 데이터 효율적 저장(JDBC Batch Insert)

- [x] 청크 단위 처리: 메모리 사용량 제어

- [x] 운영 종료 시 flush 보장 (`graceful shutdown`)


## Sequence Diagram

![Image](https://github.com/user-attachments/assets/6550d18a-c620-472d-bfe4-8bba36021758)

## Goals

> 이 프로젝트는 단순한 기술 시연을 넘어,  
**운영 환경에서도 적용 가능한 '순수 Java 기반 고신뢰 로깅 시스템' 구축**을 목표로 합니다.

- 외부 인프라가 없는 환경에서도 초당 10,000건 이상의 변경 로그 처리 가능
- 실패 시 재시도 및 flush 보장으로 신뢰성 확보
- 필드 단위 변경 사항(JSON diff)까지 명확히 추적
- 추후 Web 기반 로그 뷰어까지 제공하여 운영 편의성 확보
- 선택적으로 OpenSearch 연동하여 hybrid 구조 확장 가능

## Roadmap: Towards Lock-Free, Wait-Free Logging

현재 구조는 `BlockingQueue` 기반으로 안정성을 확보하고 있으나,  
향후에는 LMAX Disruptor처럼 **GC 최소화 + lock-free 처리 구조**로 발전시킬 계획입니다.

- **사용 중인 로그 항목만 메모리에 올리는 RingBuffer 구조**
- **CAS 기반 lock-free, wait-free 이벤트 처리**
- **객체 재사용을 통한 GC 압력 최소화**
- **TPS 50,000+, P99 latency 10ms 미만 목표**

---

## Test Results

### 부하 테스트 환경

- **도구**: k6
- **기간**: 총 20분
- **시나리오**: 세 가지 엔티티에 대해 Insert/Update 동시 실행
- **로그 생성**: 26만 건 이상, 실패 없이 모두 생성
- **지표 수집**: Prometheus + Grafana

### 부하 테스트 결과

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

### JVM 메모리 사용 비교

![Image](https://github.com/user-attachments/assets/d5aab13c-9da2-4b5c-b6a2-f951931cc041)
| 구분 | G1 Eden Peak | G1 Old Gen |
| ------------------ | ------------ | ---------- |
| 단건 Insert | 300 MB | 40 ~ 50 MB |
| JDBC Batch Insert | 30 MB | 40 ~ 50 MB |

- 배치 Insert 적용 전 평균 힙 사용: 180 MB
- 적용 후 평균 힙 사용: 70 MB (약 2.5배 효율화)

### 안정성 및 스루풋

##### 로그 유실 건수(0건)

![Image](https://github.com/user-attachments/assets/f5bdb9f2-df6f-4b94-b22c-e59f4ba371b8)

##### 처리량(max TPS 655)

![Image](https://github.com/user-attachments/assets/90cda428-50c1-44c9-890b-d80c46154f32)

- **유실 로그(Drop)**: 0건
- **평균 처리량**: 초당 400 ~ 500건
- **최대 처리량**: 초당 655건
- **오류 발생 빈도**: 0건

### 큐 동작 및 지연 시간

##### Queue 크기변화(이상치 없음)

![Image](https://github.com/user-attachments/assets/16ea0fd0-62fe-4a9b-a211-f1c973e898d1)

##### 지연시간(stable)

![Image](https://github.com/user-attachments/assets/9142e2f6-29a2-45ed-b50f-879ab1dd9991)

- **배치 사이즈**: 1,000건
- **큐 최대 사이즈**: 100,000건
- **큐 길이**: 배치 처리 전 최대 1,000 → 처리 후 초기화 반복
- **로그 큐 입력 지연**: 초기에는 높으나 곧 안정화
- **배치 Insert 지연**: 초기 설정 시간 소모 후 0.04 ~ 0.045 초 유지  
