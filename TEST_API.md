# 부하 테스트 API 문서

이 문서는 부하 테스트를 위한 API 엔드포인트들을 설명합니다.

## 사용자 API (`/api/test/users`)

### 사용자 생성

- **POST** `/api/test/users`
- **Request Body**:
  ```json
  {
    "name": "테스트 사용자",
    "email": "test@example.com",
    "phoneNumber": "010-1234-5678"
  }
  ```
- **Response**: 생성된 사용자 정보

### 로그인 업데이트

- **PUT** `/api/test/users/{id}/login`
- **Response**: 업데이트된 사용자 정보

### 사용자 활성화

- **PUT** `/api/test/users/{id}/activate`
- **Response**: 활성화된 사용자 정보

### 사용자 비활성화

- **PUT** `/api/test/users/{id}/deactivate`
- **Response**: 비활성화된 사용자 정보

## 주문 API (`/api/test/orders`)

### 주문 생성

- **POST** `/api/test/orders`
- **Request Body**:
  ```json
  {
    "orderNumber": "ORD-2024-001",
    "totalAmount": 50000,
    "user": {
      "id": 1
    }
  }
  ```
- **Response**: 생성된 주문 정보

### 주문 상태 업데이트

- **PUT** `/api/test/orders/{id}/status?status={status}`
- **Status Values**: CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
- **Response**: 업데이트된 주문 정보

### 주문 금액 업데이트

- **PUT** `/api/test/orders/{id}/amount?amount={amount}`
- **Response**: 업데이트된 주문 정보

## 상품 API (`/api/test/products`)

### 상품 생성

- **POST** `/api/test/products`
- **Request Body**:
  ```json
  {
    "name": "테스트 상품",
    "price": 10000,
    "description": "테스트 상품 설명",
    "stockQuantity": 100
  }
  ```
- **Response**: 생성된 상품 정보

### 재고 수량 업데이트

- **PUT** `/api/test/products/{id}/stock?quantity={quantity}`
- **Response**: 업데이트된 상품 정보

### 상품 활성화

- **PUT** `/api/test/products/{id}/activate`
- **Response**: 활성화된 상품 정보

### 상품 비활성화

- **PUT** `/api/test/products/{id}/deactivate`
- **Response**: 비활성화된 상품 정보

## 응답 형식

모든 API는 다음과 같은 응답 형식을 따릅니다:

### 성공 응답

```json
{
  "id": 1
  // 엔티티별 필드들...
}
```

### 에러 응답

```json
{
  "status": 404,
  "message": "Resource not found"
}
```

## 부하 테스트 시나리오 예시

1. **사용자 생성 및 상태 변경**

   - 사용자 생성
   - 로그인 업데이트
   - 활성화/비활성화 상태 변경

2. **주문 생성 및 상태 변경**

   - 주문 생성
   - 주문 상태 업데이트 (CREATED → PAID → SHIPPED → DELIVERED)
   - 주문 금액 변경

3. **상품 관리**

   - 상품 생성
   - 재고 수량 변경
   - 상품 활성화/비활성화

4. **복합 시나리오**
   - 사용자 생성 → 주문 생성 → 주문 상태 변경
   - 상품 생성 → 재고 변경 → 주문 생성
