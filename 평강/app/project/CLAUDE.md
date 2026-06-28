# 프로젝트 컨텍스트

Spring Boot 4.x + Kotlin 2.2.21 + Gradle 멀티모듈 헥사고널 아키텍처.
도메인: product, cart, user

---

## 모듈 구조

```
application-api          ← Spring Boot 진입점 (@SpringBootApplication)
shared                   ← CustomException, ErrorCode, 공통 응답 타입
product/
  model                  ← 도메인 엔티티 (순수 Kotlin, 프레임워크 의존 없음)
  infrastructure         ← 포트 인터페이스 (ProductRepository)
  service                ← 유스케이스 (ProductService)
  api                    ← REST 컨트롤러 (inbound adapter)
  repository-jpa         ← JPA 어댑터 (outbound adapter)
  schema                 ← JPA 엔티티 + SQL 마이그레이션
cart/
  model                  ← 도메인 엔티티 + ProductSummary (cart가 자체 정의한 product 뷰)
  infrastructure         ← 포트 인터페이스 (CartRepository, CartItemRepository, ProductQueryPort)
  service                ← 유스케이스 (CartService)
  api                    ← REST 컨트롤러
  repository-jpa         ← JPA 어댑터 + ProductQueryAdapter (product:infrastructure 인터페이스만 의존)
  schema                 ← JPA 엔티티 + SQL 마이그레이션
user/
  model                  ← 도메인 엔티티
  infrastructure         ← 포트 인터페이스 (UserRepository, UserValidationPort)
  repository-jpa         ← JPA 어댑터
  schema                 ← JPA 엔티티 + SQL 마이그레이션
```

## SQL 마이그레이션 위치

각 도메인 schema 모듈에 있음. application-api에는 없음.
Flyway가 `classpath:db/migration`을 스캔할 때 모든 모듈 JAR에서 자동 수집.

```
product/schema/src/main/resources/db/migration/V1__create_products.sql
user/schema/src/main/resources/db/migration/V2__create_users.sql
cart/schema/src/main/resources/db/migration/V3__create_carts.sql
```

---

## 해결한 문제들

### 1. Gradle GAV 충돌
모든 서브모듈이 동일한 `group:name:version`을 가져서 Gradle이 의존성을 하나로 collapsed → 컴파일 에러.

**해결**: `build.gradle.kts` root의 `allprojects`에서 도메인별 group 분리.
```kotlin
val domain = project.path.removePrefix(":").split(":").firstOrNull()
group = if (project.path.contains(":")) "com.eventpurchase.$domain" else "com.eventpurchase"
```
+ `archivesName.set(project.path.removePrefix(":").replace(":", "-"))`

### 2. cross-domain 어댑터 위치 재배치
- `ProductQueryPort` → `cart:infrastructure` (cart가 필요한 계약을 cart가 정의)
- `ProductQueryAdapter` → `cart:repository-jpa` (product:infrastructure 인터페이스만 의존)
- `UserValidationPort` → `user:infrastructure` (user가 제공하는 계약)
- `UserValidationAdapter` → `user:repository-jpa`
- product 모듈은 cart 모듈을 전혀 모름 → 피처 간 단방향 의존 유지

### 3. Kotlin 스마트 캐스트 에러 (Kotlin 2.x)
외부 모듈의 `val` 프로퍼티는 스마트 캐스트 불가.
```kotlin
// 전
cartItemRepository.findByCartIdAndProductId(cart.id!!, productId)  // 에러
// 후
val cartId = cart.id!!
cartItemRepository.findByCartIdAndProductId(cartId, productId)
```

### 4. Flyway 미실행 (Spring Boot 4.x)
Spring Boot 4.x에서 auto-configuration이 기능별 모듈로 분리됨.
`flyway-core`만 추가하면 `FlywayAutoConfiguration`이 동작 안 함.

**해결**: `application-api/build.gradle.kts`에 추가
```kotlin
implementation("org.springframework.boot:spring-boot-flyway")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
```

### 5. kotlin-reflect 누락
Spring Data JPA가 Kotlin 엔티티 생성자 탐색 시 `kotlin-reflect` 필요.

**해결**: `application-api/build.gradle.kts`에 추가
```kotlin
implementation("org.jetbrains.kotlin:kotlin-reflect")
```

---

## 수정할 것 (아키텍처 개선)

### 1순위: `infrastructure` 모듈 이름
현재 `{domain}:infrastructure`가 포트(인터페이스)를 담고 있음.
헥사고널에서 infrastructure는 어댑터 계층을 의미하므로 이름 혼란.
→ `{domain}:port` 또는 `{domain}:application-port`로 rename 검토.

### 3순위: Cart aggregate 일관성
`Cart` 모델이 `cartItems: List<CartItem>`를 들고 있는데
`CartService`가 `cartItemRepository`를 직접 조작해서 aggregate 의미가 없음.
Cart를 aggregate root로 쓰려면 Cart를 통해서만 item을 조작하거나,
아니면 `Cart`에서 `cartItems` 필드 제거하고 단순 엔티티로 취급.

### 4순위: BaseTimeEntity 중복 제거
`product:repository-jpa`, `cart:repository-jpa`, `user:repository-jpa`에 각각 존재.
→ `shared` 모듈로 이동.

---

## 주요 의존성 (application-api/build.gradle.kts)

```kotlin
implementation("org.jetbrains.kotlin:kotlin-reflect")
implementation("org.springframework.boot:spring-boot-flyway")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
```

## 로컬 실행

- Docker: `docker compose up -d`
- IntelliJ: `local` 프로파일로 실행
- Gradle sync 후 실행할 것 (build.gradle.kts 변경 시 반드시 sync)

## Spring Boot 4.x 주의사항

- auto-configuration이 기능별 모듈로 분리됨 (`spring-boot-flyway`, `spring-boot-hibernate` 등)
- `spring-boot-autoconfigure` 하나에 다 있던 Spring Boot 3.x와 다름
- 새 기능 추가 시 별도 autoconfigure 모듈 필요한지 확인
