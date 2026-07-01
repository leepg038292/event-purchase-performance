# 프로젝트 컨텍스트

Spring Boot 4.x + Kotlin 2.2.21 + Gradle 멀티모듈 헥사고널 아키텍처.
도메인: product, cart, user

---

## 모듈 구조

```
application-api          ← Spring Boot 진입점 (@SpringBootApplication), GlobalExceptionHandler
shared                   ← BaseTimeEntity, 공통 응답 타입 (SuccessResponse, ErrorResponse, CursorResponse)
product/
  model                  ← 도메인 엔티티 (순수 Kotlin, 외부 의존 없음)
  exception              ← ProductNotFoundException (순수 Kotlin, 외부 의존 없음)
  infrastructure         ← 포트 인터페이스 (ProductRepository, ProductLookupPort)
  service                ← 유스케이스 (ProductService)
  api                    ← REST 컨트롤러 (inbound adapter)
  repository-jpa         ← JPA 어댑터 (outbound adapter)
  schema                 ← JPA 엔티티 + SQL 마이그레이션
cart/
  model                  ← 도메인 엔티티 + ProductSummary (cart가 자체 정의한 product 뷰)
  exception              ← CartItemNotFoundException, ProductNotAvailableException (순수 Kotlin, 외부 의존 없음)
  infrastructure         ← 포트 인터페이스 (CartRepository, ProductQueryPort)
  service                ← 유스케이스 (CartService)
  api                    ← REST 컨트롤러
  repository-jpa         ← JPA 어댑터 + ProductQueryAdapter (product:infrastructure 인터페이스만 의존)
  schema                 ← JPA 엔티티 + SQL 마이그레이션
user/
  model                  ← 도메인 엔티티 (순수 Kotlin, 외부 의존 없음)
  exception              ← UserNotFoundException (순수 Kotlin, 외부 의존 없음)
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
product/schema/src/main/resources/db/migration/V4__add_product_constraints.sql
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
- `ProductQueryAdapter` → `cart:repository-jpa`
- `UserValidationPort` → `user:infrastructure` (user가 제공하는 계약)
- `UserValidationAdapter` → `user:repository-jpa`
- product 모듈은 cart 모듈을 전혀 모름 → 피처 간 단방향 의존 유지

중요: cart는 product 도메인 모델(`product:model`의 `Product`)을 알면 안 된다.
`ProductRepository.findById()`는 반환 타입이 `Product?`이므로 cart에서 직접 의존하면 `product:model`이 필요해져 경계가 깨진다.

**현재 방식**
- cart 내부 계약: `cart:infrastructure`의 `ProductQueryPort`
- product가 외부에 제공하는 조회 계약: `product:infrastructure`의 `ProductLookupPort`
- 실제 연결 어댑터: `cart:repository-jpa`의 `ProductQueryAdapter`

흐름:
```text
CartService
→ ProductQueryPort (cart가 정의한 필요한 상품 뷰)
→ ProductQueryAdapter
→ ProductLookupPort (product 도메인 Product를 노출하지 않는 조회 전용 계약)
→ ProductPersistenceAdapter
```

`ProductLookupPort`는 `ProductLookupResult`를 반환한다.
이 타입은 product 도메인 엔티티가 아니라 cart에 넘겨줄 최소 상품 조회 결과 DTO다.

금지:
```kotlin
// cart 쪽에서 금지
import com.eventpurchase.project.product.Product
import com.eventpurchase.project.product.ProductRepository
```

허용:
```kotlin
// cart:repository-jpa의 ProductQueryAdapter에서 허용
import com.eventpurchase.project.product.ProductLookupPort
```

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

### 6. CartItem 소유권 검증
`CartItemEntity`에는 `userId`가 없고 `cartId`만 있다.
따라서 `cartItemId`만으로 수정/삭제하면 다른 유저의 장바구니 아이템을 건드릴 수 있다.

**해결**: `CartItemJpaRepository.findByIdAndUserId(cartItemId, userId)`를 JPQL join으로 구현.

```kotlin
@Query("""
    select ci
    from CartItemEntity ci
    join CartEntity c on c.id = ci.cartId
    where ci.id = :cartItemId
      and c.userId = :userId
""")
fun findByIdAndUserId(
    @Param("cartItemId") id: Long,
    @Param("userId") userId: Long
): CartItemEntity?
```

의미:
```text
cart_items.id = cartItemId 인 아이템을 찾되,
그 아이템의 cart_id로 carts와 연결했을 때
carts.user_id = userId 인 경우에만 반환한다.
```

서비스에서는 수정/삭제 시 `findById(cartItemId)`를 쓰면 안 된다.
반드시 `findByIdAndUserId(cartItemId, userId)`로 소유권을 확인한다.

```kotlin
val item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
    ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)
```

검증 후 응답 빌드는 `item.cartId`를 사용하면 cart 재조회가 필요 없다.

### 7. Product 검증 추가
`Product` 도메인에 값 자체로 판단 가능한 검증을 추가했다.

```kotlin
require(productName.isNotBlank())
require(brand.isNotBlank())
require(category.isNotBlank())
require(price >= BigDecimal.ZERO)
require(discountRate == null || (discountRate >= BigDecimal.ZERO && discountRate <= BigDecimal("100")))
require(rating == null || (rating >= BigDecimal.ZERO && rating <= BigDecimal("5")))
require(reviewCount == null || reviewCount >= 0)
```

DB에는 `V4__add_product_constraints.sql`로 다음 제약을 추가했다.

```text
uk_products_source_url
chk_products_review_count_non_negative
chk_products_product_name_not_blank
chk_products_brand_not_blank
chk_products_category_not_blank
```

주의: 이미 적용된 Flyway 마이그레이션은 수정하지 않는다.
V1이 적용된 뒤 제약을 추가할 때는 반드시 새 버전 파일을 만든다.

---

## 수정할 것 (아키텍처 개선)

### 1순위: `infrastructure` 모듈 이름
현재 `{domain}:infrastructure`가 포트(인터페이스)를 담고 있음.
헥사고널에서 infrastructure는 어댑터 계층을 의미하므로 이름 혼란.
→ `{domain}:port` 또는 `{domain}:application-port`로 rename 검토.

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

---

## 2026-06-30 작업 정리

### 1. 현재 구현 분석 문서 추가
현재 코드 기준으로 도메인, 서비스, 어댑터, JPA 엔티티, DB 제약조건, 검증 책임을 정리한 문서를 추가했다.

```text
IMPLEMENTATION_ANALYSIS.md
```

핵심 기준:
```text
도메인 객체 값만으로 판단 가능한 규칙 → 도메인 검증
외부 상태/DB 조회가 필요한 규칙 → 서비스에서 포트로 검증
무결성 최후 방어선 → DB 제약조건
```

### 2. Product 도메인 검증 추가
`Product` 도메인에 기본 값 검증을 추가했다.

```kotlin
require(productName.isNotBlank())
require(brand.isNotBlank())
require(category.isNotBlank())
require(price >= BigDecimal.ZERO)
require(discountRate == null || (discountRate >= BigDecimal.ZERO && discountRate <= BigDecimal("100")))
require(rating == null || (rating >= BigDecimal.ZERO && rating <= BigDecimal("5")))
require(reviewCount == null || reviewCount >= 0)
```

주의:
```text
String non-null 타입에 require(productName != null)는 의미 없음.
공백 방지는 productName.isNotBlank() 사용.
nullable 필드는 "null이면 허용, 값이 있으면 범위 검증" 형태로 작성.
```

### 3. Product DB 제약 추가
V1 마이그레이션은 수정하지 않고 새 마이그레이션을 추가했다.

```text
product/schema/src/main/resources/db/migration/V4__add_product_constraints.sql
```

추가된 제약:

```text
uk_products_source_url
chk_products_review_count_non_negative
chk_products_product_name_not_blank
chk_products_brand_not_blank
chk_products_category_not_blank
```

주의:
```text
이미 적용된 Flyway V1은 수정하지 않는다.
기존 테이블에 제약을 추가할 때는 V4 같은 새 버전 파일을 만든다.
NOT NULL은 이미 V1에 있으므로 공백 문자열 방지는 CHECK(length(trim(...)) > 0)로 처리한다.
```

### 4. CartItem 도메인 검증 확인
`CartItem`에는 이미 다음 검증이 들어가 있다.

```kotlin
require(quantity > 0)
require(productId > 0)
require(cartId > 0)
```

`copy(...)`를 사용해도 data class의 `init`은 다시 실행되므로 검증은 동작한다.
다만 의도를 드러내려면 서비스에서 `copy` 대신 도메인 메서드를 사용한다.

```kotlin
existing.increase(quantity)
item.changeQuantity(quantity)
```

### 5. CartItem 수정/삭제 소유권 검증 추가
기존 문제:

```kotlin
cartItemRepository.findById(cartItemId)
```

이 방식은 `cartItemId`만 알면 다른 유저의 장바구니 아이템도 수정/삭제할 수 있다.

해결:

```kotlin
cartItemRepository.findByIdAndUserId(cartItemId, userId)
```

JPA 쿼리:

```kotlin
@Query("""
    select ci
    from CartItemEntity ci
    join CartEntity c on c.id = ci.cartId
    where ci.id = :cartItemId
      and c.userId = :userId
""")
fun findByIdAndUserId(
    @Param("cartItemId") id: Long,
    @Param("userId") userId: Long
): CartItemEntity?
```

쿼리 의미:
```text
cart_items에는 user_id가 없으므로 carts와 조인한다.
조인 조건은 carts.id = cart_items.cart_id.
cart_items.id = cartItemId 이면서 carts.user_id = userId인 경우에만 CartItemEntity를 반환한다.
없으면 null을 반환하고 서비스에서 CART_ITEM_NOT_FOUND로 처리한다.
```

수정/삭제 서비스는 검증 후 `item.cartId`로 응답을 다시 빌드한다.

```kotlin
val item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
    ?: throw CustomException(ErrorCode.CART_ITEM_NOT_FOUND)

return buildView(item.cartId, userId)
```

### 6. Product 도메인 누수 방지
중요한 아키텍처 규칙:

```text
cart는 product:model의 Product를 알면 안 된다.
```

잘못된 방향:

```kotlin
// cart 쪽에서 금지
import com.eventpurchase.project.product.Product
import com.eventpurchase.project.product.ProductRepository
```

이유:
`ProductRepository.findById()`는 반환 타입이 `Product?`라서 cart가 이 포트를 직접 쓰면 `product:model` 의존이 필요해진다.

해결:
product infrastructure에 조회 전용 계약을 추가했다.

```kotlin
interface ProductLookupPort {
    fun findSummaryById(id: Long): ProductLookupResult?
}
```

`ProductLookupResult`는 product 도메인 엔티티가 아니라 cart에 넘겨줄 최소 조회 결과 DTO다.

흐름:

```text
CartService
→ ProductQueryPort
→ ProductQueryAdapter
→ ProductLookupPort
→ ProductPersistenceAdapter
```

### 7. 컴파일 검증
다음 컴파일을 확인했다.

```bash
./gradlew :product:model:compileKotlin
./gradlew :cart:repository-jpa:compileKotlin :cart:service:compileKotlin
```

결과:

```text
BUILD SUCCESSFUL
```

주의:
Gradle wrapper cache가 `~/.gradle`에 있어 sandbox 안에서는 권한 오류가 발생할 수 있다.
이 경우 권한 승인 후 실행해야 한다.

---

## 2026-07-01 작업 정리

### 1. BaseTimeEntity shared 모듈로 이동

**이전 문제:**
`BaseTimeEntity`가 `product:repository-jpa`, `cart:repository-jpa`, `user:repository-jpa`에 각각 동일한 코드로 존재했다.

**이유:**
`BaseTimeEntity`는 JPA 관심사(`@MappedSuperclass`, `@CreatedDate`)이므로 JPA 어댑터 계층(`repository-jpa`)끼리 공유하는 것이 맞다.
도메인(`model`)이 JPA를 알면 안 되므로 `model` 모듈이 아닌 `shared`에 위치시켰다.
`repository-jpa` 모듈들은 이미 모두 `shared`를 의존하고 있었으므로 추가 의존성 없이 공유 가능하다.

**결과:**
- `shared/BaseTimeEntity.kt` 신규 생성
- `shared/build.gradle.kts`에 `spring-boot-starter-data-jpa` 추가
- 각 도메인 `repository-jpa`의 `BaseTimeEntity.kt` 삭제

### 2. model 모듈 순수화 (shared 의존 제거)

**이전 문제:**
`cart:model`, `product:model`, `user:model`이 모두 `shared`를 의존하고 있었는데,
실제로 `shared`에서 import하는 코드가 한 줄도 없었다.
`shared`에 JPA(`spring-boot-starter-data-jpa`)가 들어가면서 도메인 모델이 JPA를 전이 의존하게 되는 문제가 생겼다.

**이유:**
헥사고널 아키텍처에서 도메인(`model`)은 외부 프레임워크를 전혀 몰라야 한다.
`BaseTimeEntity`는 JPA 어댑터 계층의 관심사이므로 `repository-jpa`가 `shared`를 의존하면 충분하다.
`model`이 `shared`를 의존할 이유가 없었다.

**결과:**
- `cart:model`, `product:model`, `user:model`의 `build.gradle.kts`에서 `shared` 의존 제거
- 세 model 모듈 모두 외부 의존성 없는 순수 Kotlin이 됨

### 3. Cart aggregate root 구현

**이전 문제:**
`Cart` 도메인 모델이 `cartItems: List<CartItem>`을 갖고 있어 aggregate root처럼 생겼지만,
`CartService`가 `cartItemRepository`를 직접 조작해서 `Cart.cartItems`는 채워지기만 하고 읽히는 곳이 없었다.
- `CartPersistenceAdapter`가 `Cart`를 로드할 때 불필요한 DB 쿼리(`findAllByCartId`)를 날려 `cartItems`를 채움
- `CartService`는 받은 `Cart`에서 `id`, `userId`만 꺼내고 `cartItems`는 버림
- item 추가/수정/삭제 로직이 서비스에서 `copy()`로 직접 구현되어 도메인 규칙이 서비스에 노출됨

**이유:**
장바구니에 아이템을 담는 행위는 `Cart`가 책임져야 한다.
`Cart`가 진짜 aggregate root가 되면 서비스는 의도만 표현하고, 도메인 규칙은 도메인 안에 캡슐화된다.
소유권 검증도 `userId`로 Cart를 먼저 로드하면 그 안의 item이 이미 해당 유저 것이므로 별도 JOIN 쿼리가 필요 없어진다.

**결과:**
- `Cart`에 `addItem()`, `removeItem()`, `changeItemQuantity()`, `clear()` 도메인 메서드 추가
- `removeItem()`, `changeItemQuantity()` 안에서 `require()`로 item 존재 여부 검증 (도메인 규칙 캡슐화)
- `CartRepository`에 `save(cart: Cart): Cart` 추가
- `CartPersistenceAdapter.save()` 구현 (기존 items와 diff해서 삭제/upsert)
- `CartService`에서 `cartItemRepository` 의존 완전 제거
- `CartItemRepository` 포트 + `CartItemPersistenceAdapter` 삭제
- `CartItemJpaRepository`의 `findByIdAndUserId` JOIN 쿼리 삭제 (소유권이 aggregate 로드로 보장됨)
- `buildView`가 `cartItemRepository` 대신 `cart.cartItems`를 직접 사용

```kotlin
// 변경 전 — 서비스가 도메인 내부를 직접 조작
cartItemRepository.save(existing.copy(quantity = existing.quantity + quantity))

// 변경 후 — Cart에 위임
val updated = cartRepository.save(cart.addItem(productId, quantity))
```

### 4. 예외 처리 방식 변경 (CustomException → 도메인별 exception 모듈)

**이전 문제:**
`CustomException(ErrorCode.CART_ITEM_NOT_FOUND)`처럼 `ErrorCode`에 HTTP 상태코드(`status: Int`)가 박혀 있었다.
서비스가 `CustomException`을 던지는 순간 HTTP 404 같은 외부 세계 개념을 알게 된다.
헥사고널에서 서비스는 "무슨 일이 일어났는지"만 알아야 하고, HTTP 코드는 어댑터(핸들러)가 결정해야 한다.

**이유:**
도메인/서비스는 HTTP를 몰라야 한다.
예외 클래스 자체가 상황을 표현하고(`CartItemNotFoundException`), HTTP 매핑은 `GlobalExceptionHandler`(어댑터 계층)에서만 결정한다.
예외 클래스는 `RuntimeException`만 상속하면 되므로 프레임워크 의존 없이 순수 Kotlin으로 만들 수 있다.
도메인별 `exception` 서브모듈로 분리하면 서비스가 자기 도메인 예외만 의존하게 된다.

**크로스 도메인 예외 처리:**
`cart:service`가 product 조회 실패 시 `ProductNotFoundException`(product 도메인 것)을 쓰면 cart → product:model 의존이 생긴다.
대신 `ProductNotAvailableException`을 `cart:exception`에 정의해서 cart 관점의 예외로 표현한다.

**결과:**
```
product/exception/ → ProductNotFoundException
cart/exception/    → CartItemNotFoundException, ProductNotAvailableException
user/exception/    → UserNotFoundException
```
- `shared`에서 `CustomException`, `ErrorCode` 삭제
- `ErrorResponse`에서 `ErrorCode` 참조 제거
- `GlobalExceptionHandler`가 각 예외 클래스별로 HTTP 상태코드를 직접 결정
- `IllegalArgumentException`(도메인 `require()` 실패) → 400으로 처리
