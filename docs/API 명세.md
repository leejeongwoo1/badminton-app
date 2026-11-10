# API 명세 (MVP 핵심 흐름)

> 범위: 회원가입/로그인 → 체크인 → 매칭 → 게임 → 점수 제출 → 결과 확정
> 체육관 등록/관리 API는 마지막에 목록만 정리 (다음 단계에서 상세화)

## 0. 공통 규칙

| 항목 | 규칙 |
|---|---|
| Base URL | `/api/v1` |
| 인증 | `Authorization: Bearer {accessToken}` (회원가입·로그인 제외 전부) |
| 형식 | JSON, 시각은 ISO-8601 (`2026-11-01T19:05:00+09:00`) |
| 에러 응답 | `{ "code": "ALREADY_CHECKED_IN", "message": "이미 다른 체육관에 체크인되어 있어요." }` |

### 공통 상태 코드

| HTTP | 의미 | 이 앱에서 주로 쓰는 경우 |
|---|---|---|
| 200 / 201 | 성공 / 생성됨 | |
| 400 | 요청 값이 잘못됨 | 점수 규칙 위반, 성별과 맞지 않는 모드 |
| 401 | 로그인 필요 | 토큰 없음/만료 |
| 403 | 권한 없음 | 반경 밖(체크인 시), 운영 시간 아님, 남의 게임 |
| 404 | 대상 없음 | |
| 409 | **현재 상태에서 할 수 없는 요청** | 상태 다이어그램에 없는 전이 (예: 게임 중에 체크아웃) |

> **409 = 상태 다이어그램 위반.** 모든 상태 변경 API는 "지금 상태가 X일 때만 Y로 바꿔라" 조건부 UPDATE로 처리하고, 바뀐 행이 0개면 409를 돌려준다. 동시 요청도 이 방식으로 하나만 성공한다.

---

## 1. 회원 / 인증

### `POST /auth/signup` 회원가입

```json
// request
{ "email": "a@b.com", "password": "****", "nickname": "정우",
  "gender": "MALE", "grade": "C" }
// 201 response
{ "userId": 1, "nickname": "정우", "grade": "C", "mmr": 1200 }
```

- `grade`: `BEGINNER | D | C | B | A` — **가입 시 1회, 수정 불가**
- `mmr` 초기값 = 급수별 기준값 (예: 초심 800 / D 1000 / C 1200 / B 1400 / A 1600)
- 에러: `409 DUPLICATE_EMAIL`, `409 DUPLICATE_NICKNAME`

### `POST /auth/login` 로그인

```json
// request
{ "email": "a@b.com", "password": "****" }
// 200 response
{ "accessToken": "...", "refreshToken": "..." }
```

### `POST /auth/refresh` 토큰 재발급

### `PUT /users/me/push-token` 푸시 토큰 등록

```json
{ "fcmToken": "..." }
```

- 앱 실행 시마다 호출 (토큰은 바뀔 수 있음)

### `GET /users/me` 내 정보

```json
{ "userId": 1, "nickname": "정우", "gender": "MALE", "grade": "C",
  "mmr": 1234, "gamesPlayed": 12 }
```

---

## 2. 체육관 조회

### `GET /gyms?lat={lat}&lng={lng}&radiusKm=5` 주변 체육관 목록

```json
[{ "gymId": 3, "name": "OO체육관", "distanceM": 420,
   "openTime": "06:00", "closeTime": "22:00", "isOpenNow": true,
   "courtCount": 6, "checkedInCount": 14 }]
```

### `GET /gyms/{gymId}/status` 체육관 현황

```json
{ "gymId": 3, "checkedInCount": 14, "waitingCount": 5,
  "courts": [
    { "courtNumber": 1, "status": "IN_USE", "startedAt": "..." },
    { "courtNumber": 2, "status": "IN_USE", "startedAt": "..." },
    { "courtNumber": 3, "status": "AVAILABLE" } ],
  "waitingGames": 1 }
```

---

## 3. 체크인 / 체크아웃

### `POST /gyms/{gymId}/check-ins` 체크인

```json
// request
{ "lat": 37.24, "lng": 127.08 }
// 201 response
{ "checkInId": 55, "gymId": 3, "status": "RESTING" }
```

| 검사 | 실패 시 |
|---|---|
| 운영 시간인가 (`open_time ~ close_time`) | `403 OUTSIDE_OPERATING_HOURS` |
| 체육관 반경 안인가 (`auth_radius_m`) | `403 OUT_OF_RANGE` |
| 진행 중인 체크인이 없는가 | `409 ALREADY_CHECKED_IN` |

- 상태 전이: (없음) → `RESTING`

### 자동 체크아웃 (API 없음)

- 위치는 **체크인할 때 한 번만** 확인한다. 그 뒤로는 위치를 추적하지 않는다.
- **마지막 활동 후 90분**이 지나면 스케줄러가 자동 체크아웃 (→ 6장)
  - 마지막 활동(`last_active_at`) = 체크인 / 매칭 시작 / 게임 시작 중 가장 최근 시각
  - 게임 중(`PLAYING`)이면 체크아웃하지 않음
- 나가기를 안 누르고 가도 문제가 적은 이유: 게임이 끝나면 휴식 상태가 되고, **다시 "매칭 시작"을 눌러야만** 매칭되므로 집에 간 사람이 매칭되는 일은 없다. 자동 체크아웃은 체육관 현황의 "체크인 인원"을 정확하게 유지하는 용도.

### `DELETE /check-ins/current` 체크아웃 (나가기)

- 상태 전이: `RESTING | QUEUED | MATCHED | AWAITING_RESULT` → `CHECKED_OUT`
  - `QUEUED`면 대기열 취소
  - `MATCHED`면 **게임 해체**, 나머지 3명은 매칭 대기로 복귀
- 에러: `409 IN_GAME` (게임 중에는 나가기 불가)

### `GET /me/current` 내 현재 상황 (앱 메인 화면용)

앱을 열거나 푸시를 받았을 때 이것 하나로 화면을 그린다.

```json
{
  "checkIn": { "checkInId": 55, "gymId": 3, "gymName": "OO체육관",
               "status": "PLAYING" },
  "queue":   null,
  "game": {
    "gameId": 301, "mode": "MIXED_DOUBLES", "status": "IN_PROGRESS",
    "courtNumber": 2, "startedAt": "...",
    "teamA": [{ "userId": 1, "nickname": "정우" }, { "userId": 7, "nickname": "민지" }],
    "teamB": [{ "userId": 9, "nickname": "철수" }, { "userId": 4, "nickname": "영희" }]
  },
  "pendingResultGameId": null
}
```

---

## 4. 매칭

### `POST /queue` 매칭 시작

```json
// request
{ "modes": ["MIXED_DOUBLES", "ANY"] }
// 201 response
{ "queueEntryId": 12, "status": "WAITING", "enqueuedAt": "..." }
```

| 검사 | 실패 시 |
|---|---|
| `modes`가 1개 이상 | `400 EMPTY_MODES` |
| 성별과 맞는 모드인가 (남자 → 여복 불가 등) | `400 INVALID_MODE_FOR_GENDER` |
| 체크인 상태가 `RESTING`인가 | `409 NOT_RESTING` |
| (특히) `AWAITING_RESULT`면 | `409 RESULT_PENDING` — **점수를 내야 다음 게임 가능** |

- 상태 전이: `RESTING` → `QUEUED`
- `last_active_at` 갱신
- 요청 직후 서버가 해당 체육관의 **매칭을 비동기로 시도** (→ 시퀀스 다이어그램 1)

### `DELETE /queue/current` 매칭 취소

- 상태 전이: `QUEUED` → `RESTING`
- 에러: `409 ALREADY_MATCHED` (취소 버튼을 누른 순간 매칭된 경우 — 동시성)

---

## 5. 게임

### `GET /games/{gameId}` 게임 상세

`GET /me/current`의 `game`과 같은 형식 + 점수 제출 현황

### 게임 시작 (API 없음)

- **시작 버튼이 없다.** 매칭 서비스가 코트를 배정하는 순간 바로 게임이 시작된다.
- 상태 전이: 게임 `WAITING_COURT` → `IN_PROGRESS` (`started_at = now`), 코트 `AVAILABLE` → `IN_USE`, 참가자 4명 `MATCHED` → `PLAYING`
- 참가자 4명의 `last_active_at` 갱신

### `POST /games/{gameId}/no-shows` 불참 신고

```json
{ "userIds": [9] }
```

- 신고는 그 게임 참가자만 가능 (`403 NOT_PARTICIPANT`)
- **게임 시작(코트 배정) 후 3분 ~ 10분 사이에만** 가능
  - 3분 전 → `409 TOO_EARLY` (도착할 시간을 줌)
  - 10분 후 → `409 TOO_LATE` (이미 게임을 치고 있으므로, 지고 있는 쪽이 게임을 없애는 악용 방지)
- 게임이 `IN_PROGRESS`일 때만 → 아니면 `409 INVALID_GAME_STATUS`
- 상태 전이: 게임 `IN_PROGRESS` → `CANCELLED` (**폐기**, 기록 안 남음), 코트 → `AVAILABLE` (→ 대기 게임에 배정)
  - 불참자: `PLAYING` → `RESTING`
  - 나머지: `PLAYING` → `QUEUED` (**원래 대기 시작 시각을 유지**해서 다시 앞쪽 순번으로)

### `POST /games/{gameId}/scores` 점수 제출

```json
// request
{ "teamAScore": 21, "teamBScore": 18 }
// 201 response
{ "submissionId": 88, "gameStatus": "COLLECTING_SCORES",
  "resultDeadline": "2026-11-01T19:45:00+09:00" }
```

- **4명 모두 제출 가능**, 1인 1회 (`409 ALREADY_SUBMITTED` → 수정은 PUT)
- 점수 규칙 검사 (21점제) → 실패 시 `400 INVALID_SCORE`
  - 이긴 쪽 21점 & 진 쪽 19점 이하, 또는
  - 20:20 이후 2점 차 (22:20 … 29:27), 또는
  - 30:29
- **첫 제출이 게임 종료 신호**:
  - 게임 `IN_PROGRESS` → `COLLECTING_SCORES`, `ended_at = now`
  - 코트 `IN_USE` → `AVAILABLE` → **대기 중인 게임에 코트 배정**
  - 참가자 4명 `PLAYING` → `AWAITING_RESULT`
- 제출한 본인: `AWAITING_RESULT` → `RESTING` (바로 다음 매칭 가능)
- 4명이 모두 제출했고 모두 같으면 즉시 확정 (→ 아래 스케줄러와 같은 확정 처리)

### `PUT /games/{gameId}/scores/me` 내 점수 수정

- 게임이 `COLLECTING_SCORES` 또는 `DISPUTED`일 때만 → 아니면 `409 RESULT_CLOSED`
- 불일치 알림을 받은 사람이 고치는 용도

---

## 6. 스케줄러 (API 아님, 서버 내부 작업)

주기적으로 (예: 30초마다) 실행. 모든 상태 변경은 조건부 UPDATE.

| 작업 | 조건 | 처리 |
|---|---|---|
| 결과 확정 | `COLLECTING_SCORES` & `ended_at + 10분` 지남 | 입력값 모두 같음 → `CONFIRMED` + MMR 반영<br>다름 → `DISPUTED` + 입력자에게 푸시<br>0건 → `VOIDED` |
| 플레이어 해제 | 위와 같은 시점 | 그 게임의 `AWAITING_RESULT` 참가자 → `RESTING` |
| 불일치 마감 | `DISPUTED` & `ended_at + 20분` 지남 | 그 사이 모두 같아졌으면 `CONFIRMED`, 아니면 `VOIDED` |
| 방치된 게임 | `IN_PROGRESS` & `started_at + 40분` 지남 | `VOIDED`, 코트 반납, 참가자 → `RESTING` |
| 전원 이탈 | `IN_PROGRESS` & 참가자 4명 모두 체크아웃 | `VOIDED`, 코트 반납 |
| 자동 체크아웃 | `last_active_at + 90분` 지남 & `PLAYING` 아님 | `CHECKED_OUT` (체크아웃 API와 같은 처리) |
| 매칭 재시도 | 대기자가 있는 체육관 | 매칭 시도 (대기열 진입 시에도 즉시 실행) |

---

## 7. 푸시 알림 (서버 → 앱)

| type | 언제 | 받는 사람 | 문구 예시 |
|---|---|---|---|
| `MATCHED_WAITING_COURT` | 매칭됐는데 빈 코트 없음 | 4명 | "매칭됐어요! 코트가 비면 알려드릴게요" |
| `COURT_ASSIGNED` | 코트 배정 = 게임 시작 | 4명 | "2번 코트에서 바로 게임 시작!" |
| `GAME_CANCELLED` | 불참·이탈로 해체 | 남은 참가자 | "참가자 불참으로 다시 매칭할게요" |
| `SCORE_REQUEST` | 첫 점수 제출됨 | 아직 안 낸 참가자 | "점수를 확인해주세요 (10분)" |
| `SCORE_MISMATCH` | 불일치 | 점수 입력자 | "입력한 점수가 서로 달라요 (10분 안에 수정)" |
| `RESULT_CONFIRMED` | 확정 | 4명 | "21:18 승리! MMR +14" |
| `AUTO_CHECKED_OUT` | 자동 체크아웃 | 본인 | "체육관을 벗어나 체크아웃됐어요" |

- 앱은 푸시를 받으면 `GET /me/current`로 최신 상태를 다시 불러온다. (푸시는 "뭔가 바뀌었다"는 신호로만 쓰고, 내용은 항상 서버에서 조회)

---

## 8. 체육관 관리 (목록만, 다음에 상세화)

| API | 설명 |
|---|---|
| `POST /gyms` | 체육관 등록 (자동 승인, 위치 기준 중복 확인 → `409 DUPLICATE_GYM`), 등록자가 관리자가 됨 |
| `GET /me/managed-gyms` | 내가 관리하는 체육관 |
| `PATCH /gyms/{gymId}` | 정보 수정 (관리자만) |

---

## 9. 아직 정하지 않은 것

1. 매칭 알고리즘 상세 (MMR 차이 허용 범위, 오래 기다린 사람 우선 규칙)
2. MMR 계산식 (Elo 기준, K값)
