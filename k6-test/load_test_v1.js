import http from 'k6/http';
import { check, group, sleep } from 'k6';

export let options = {
  thresholds: {
    'http_req_duration': ['p(95)<500', 'avg<200'],
    'http_req_failed': ['rate<0.01'],
  },
  scenarios: {
    basic_user: {
      executor: 'constant-vus',
      vus: 10,
      duration: '2m',
    },
    basic_order: {
      executor: 'constant-vus',
      startTime: '2m',
      vus: 10,
      duration: '2m',
    },
    basic_product: {
      executor: 'constant-vus',
      startTime: '4m',
      vus: 10,
      duration: '2m',
    },
    composite_user_order: {
      executor: 'constant-vus',
      startTime: '6m',
      vus: 20,
      duration: '3m',
    },
    composite_product_order: {
      executor: 'constant-vus',
      startTime: '9m',
      vus: 20,
      duration: '3m',
    },
    full_concurrency: {
      executor: 'ramping-vus',
      startTime: '12m',
      stages: [
        { duration: '1m', target: 0 },
        { duration: '4m', target: 100 },
        { duration: '4m', target: 100 },
        { duration: '1m', target: 0 },
      ],
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HEADERS = { 'Content-Type': 'application/json' };
let callCount = 0;     // 전역 호출 카운터

function createUser() {
  callCount += 1;
  const uniqueSuffix = `${__VU}-${__ITER}-${callCount}`;

  let payload = JSON.stringify({
    name: `유저-${uniqueSuffix}`,
    email: `user${uniqueSuffix}@example.com`,
    phoneNumber: `010-0000-${('000'+__ITER).slice(-4)}`,
  });
  let res = http.post(`${BASE_URL}/api/test/users`, payload, { headers: HEADERS });
  check(res, { 'createUser 2xx': r => r.status === 200 || r.status === 201 });
  return res.json('id');
}

function updateUserLogin(userId) {
  let res = http.put(`${BASE_URL}/api/test/users/${userId}/login`, null, { headers: HEADERS });
  check(res, { 'updateLogin 2xx': r => r.status === 200 });
}

function toggleUserActive(userId, activate = true) {
  let action = activate ? 'activate' : 'deactivate';
  let res = http.put(`${BASE_URL}/api/test/users/${userId}/${action}`, null, { headers: HEADERS });
  check(res, { [`user${action} 2xx`]: r => r.status === 200 });
}

function createOrder(userId) {
  let payload = JSON.stringify({
    orderNumber: `ORD-${__VU}-${__ITER}`,
    totalAmount: 50000,
    userId: userId,
  });
  let res = http.post(`${BASE_URL}/api/test/orders`, payload, { headers: HEADERS });
  check(res, { 'createOrder 2xx': r => r.status === 200 || r.status === 201 });
  return res.json('id');
}

function updateOrderStatus(orderId, status) {
  let res = http.put(`${BASE_URL}/api/test/orders/${orderId}/status?status=${status}`, null, { headers: HEADERS });
  check(res, { [`orderStatus ${status} 2xx`]: r => r.status === 200 });
}

function updateOrderAmount(orderId, amount) {
  let res = http.put(`${BASE_URL}/api/test/orders/${orderId}/amount?amount=${amount}`, null, { headers: HEADERS });
  check(res, { 'orderAmountChange 2xx': r => r.status === 200 });
}

function createProduct() {
  let payload = JSON.stringify({
    name: `상품-${__VU}-${__ITER}`,
    price: 10000,
    description: '테스트 상품',
    stockQuantity: 100,
  });
  let res = http.post(`${BASE_URL}/api/test/products`, payload, { headers: HEADERS });
  check(res, { 'createProduct 2xx': r => r.status === 200 || r.status === 201 });
  return res.json('id');
}

function updateProductStock(productId, qty) {
  let res = http.put(`${BASE_URL}/api/test/products/${productId}/stock?quantity=${qty}`, null, { headers: HEADERS });
  check(res, { 'productStockChange 2xx': r => r.status === 200 });
}

function toggleProductActive(productId, activate = true) {
  let action = activate ? 'activate' : 'deactivate';
  let res = http.put(`${BASE_URL}/api/test/products/${productId}/${action}`, null, { headers: HEADERS });
  check(res, { [`product${action} 2xx`]: r => r.status === 200 });
}

export default function () {
  // basic_user 시나리오
  group('Basic User Flow', () => {
    let userId = createUser();
    updateUserLogin(userId);
    toggleUserActive(userId, false);
    toggleUserActive(userId, true);
  });

  // basic_order 시나리오
  group('Basic Order Flow', () => {
    let userId = createUser(); // order 생성 위해
    let orderId = createOrder(userId);
    ['PAID','SHIPPED','DELIVERED'].forEach(status => updateOrderStatus(orderId, status));
    updateOrderAmount(orderId, 45000);
  });

  // basic_product 시나리오
  group('Basic Product Flow', () => {
    let productId = createProduct();
    updateProductStock(productId, 80);
    toggleProductActive(productId, false);
    toggleProductActive(productId, true);
  });

  // composite_user_order 시나리오
  group('Composite User-Order Flow', () => {
    let userId = createUser();
    let orderId = createOrder(userId);
    updateOrderStatus(orderId, 'PAID');
    updateUserLogin(userId);
  });

  // composite_product_order 시나리오
  group('Composite Product-Order Flow', () => {
    let productId = createProduct();
    updateProductStock(productId, 90);
    let userId = createUser();
    let orderId = createOrder(userId);
    updateOrderStatus(orderId, 'SHIPPED');
    check(http.get(`${BASE_URL}/api/test/products/${productId}`, { headers: HEADERS }), { 'getProduct 200': r => r.status === 200 });
  });

  // full_concurrency 시나리오는 ramping-vus로 자동 실행
  sleep(1);
}
