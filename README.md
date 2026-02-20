# 🍑 Peach Music Worker

Peach Music Worker는 음원 다운로드 및 트랜스코딩(형변환) 처리를 담당하는 비동기 처리 서버입니다.

메인 서버로부터 작업 요청을 받아 음원을 다운로드하고,  
스트리밍이 가능하도록 트랜스코딩을 수행합니다.

Worker 서버를 분리하여 무거운 파일 처리 작업을 독립적으로 수행함으로써  
메인 서버의 부하를 줄이고 안정적인 서비스 운영이 가능하도록 설계했습니다.

---

# 🛠 기술 스택

**Language**

- Java 17

**Back-End**

- Spring Boot 3.5.9
- Spring Web
- Spring Data JPA

**Database**

- MySQL

**Storage**

- Cloudflare R2

**Infra**

- Docker

**IDE**

- IntelliJ IDEA

---

# ⚙️ 역할 및 기능

- 음원 다운로드 처리
- 음원 트랜스코딩(형변환) 처리
- 메인 서버 요청 기반 비동기 작업 수행
- 멀티 워커 환경에서 안전한 작업 처리

---

# 📁 디렉토리 구조

```text
src/main/java/com/example/worker

├── common              # 공통 설정 및 R2 저장소 설정
│
├── domain              # 워커 처리 대상 도메인
│   ├── album
│   ├── song
│   └── songprogressingstatus   # 다운로드 및 트랜스코딩 상태 관리
│
├── worker              # 워커 핵심 로직
│   ├── controller      # 작업 요청 처리
│   ├── scheduler       # 다운로드 및 트랜스코딩 스케줄러
│   ├── service         # 다운로드 및 트랜스코딩 처리
│   └── worker          # 다운로드 및 트랜스코딩 실행
│
└── TranscoderApplication   # Worker 서버 실행 클래스

src/test/java/com/example/worker

└── worker              # 다운로드 및 트랜스코딩 테스트
```

---

# 🚧 트러블슈팅

## 멀티 워커 환경에서 동일 작업 중복 처리 문제 해결

스케줄러 작업 중 관리자가 동일 작업을 수동으로 실행할 경우, 하나의 음원이 여러 워커에 의해 중복 처리될 수 있는 문제가 있었습니다.

이를 해결하기 위해 작업 시작 시 DB 상태를 조건부 UPDATE로 변경하여, 상태 변경에 성공한 워커만 작업을 수행하도록 설계했습니다.

이를 통해 멀티 워커 환경에서도 동일 작업의 중복 수행을 방지하고 작업 상태의 정합성을 유지할 수 있었습니다.

---

# 🚀 실행 방법

## 1. 프로젝트 클론

```bash
git clone https://github.com/4realavenue/PeachMusic_Worker.git
cd PeachMusic_Worker
```

---

## 2. 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다.

```bash
touch .env
```

`.env` 예시

```env
DB_URL=jdbc:mysql://localhost:3306/peach_music
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

PORT=8081

LOG_PATH=/var/log/peachmusic-worker

STREAMING_AUDIO_PATH=/path/to/transcodeAudio

R2_ACCOUNT_ID=your_r2_account_id
R2_ACCESS_KEY_ID=your_r2_access_key_id
R2_SECRET_ACCESS_KEY=your_r2_secret_access_key

R2_BUCKET_MEDIA=your_r2_bucket_media
R2_BUCKET_ASSETS=your_r2_bucket_assets
```

---

## 3. 서버 실행

```bash
./gradlew bootRun
```

또는 IntelliJ에서

```
TranscoderApplication 실행
```

---

## 4. 서버 확인

```
http://localhost:8081
```

---

## 🧪 테스트

```bash
./gradlew test
```