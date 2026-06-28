# 아키텍처 설계 결정 기록

## 목차
1. [전체 전환 배경](#1-전체-전환-배경)
2. [Java → Kotlin 전환](#2-java--kotlin-전환)
3. [단일 모듈 → 멀티 모듈 전환](#3-단일-모듈--멀티-모듈-전환)
4. [기능별 모듈 분리 (Feature-First)](#4-기능별-모듈-분리-feature-first)
5. [단일 피처 모듈 → 서브 모듈 분리](#5-단일-피처-모듈--서브-모듈-분리)
6. [Classpath와 서브 모듈의 동작 원리](#6-classpath와-서브-모듈의-동작-원리)
7. [빌드 타임 vs 런타임 vs 테스트](#7-빌드-타임-vs-런타임-vs-테스트)
8. [인프라 의존성 격리 확장 (Redis, Kafka)](#8-인프라-의존성-격리-확장-redis-kafka)
9. [CartItem의 Product 의존 제거](#9-cartitem의-product-의존-제거)
10. [UseCase 인터페이스 통합](#10-usecase-인터페이스-통합)
8. [DTO 위치 결정 (api 레이어)](#8-dto-위치-결정-api-레이어)
9. [Schema = SQL 마이그레이션](#9-schema--sql-마이그레이션)
10. [SQL 마이그레이션 위치](#10-sql-마이그레이션-위치)
11. [모듈러 모놀리스 vs MSA](#11-모듈러-모놀리스-vs-msa)
12. [최종 모듈 구조](#12-최종-모듈-구조)
13. [의존성 흐름 요약](#13-의존성-흐름-요약)
14. [IntelliJ 런타임 클래스패스 전파 문제](#14-intellij-런타임-클래스패스-전파-문제)

---

## 1. 전체 전환 배경

### 기존 구조의 문제
기존 프로젝트는 단일 Spring Boot 모듈 안에 계층형 패키지 구조(controller / service / repository)로 이루어져 있었다. 이 구조에서는:

- 비즈니스 로직이 Spring 어노테이션과 결합되어 도메인이 인프라에 종속됨
- 어떤 기능이 외부에 제공되는지 코드만 보고 파악하기 어려움
- 피처 간 경계가 없어 `CartItem`이 `Product` 엔티티를 직접 참조하는 등 피처 간 결합 발생
- 테스트 시 Spring 컨텍스트 전체를 올려야 하는 구조

### 전환 목표
- 도메인이 Spring / JPA 등 외부 프레임워크에 의존하지 않는 순수한 상태 유지
- 피처 간 경계를 명확히 하여 한 피처의 변경이 다른 피처에 영향을 주지 않도록 설계
- 외부에 제공하는 기능(usecase)이 코드에서 명시적으로 드러나도록 구성

---

## 2. Java → Kotlin 전환

### 결정
Java + Lombok을 제거하고 Kotlin으로 전면 전환.

### 이유
**불변성(Immutability) 기본 지원**
- Java는 불변 객체를 만들기 위해 Lombok의 `@Value`, `@Builder` 등을 사용해야 하며, 이는 어노테이션 프로세서에 대한 의존을 추가함
- Kotlin `data class`는 `val` 필드로 선언하면 기본적으로 불변이고, `copy()`로 필드 일부만 변경한 새 객체를 생성할 수 있음

**헥사고널 아키텍처와의 관계**
- 도메인 객체(`Cart`, `Product` 등)가 불변이면 서비스 레이어에서 상태를 직접 변경할 수 없음
- 상태 변경은 반드시 `copy()`를 통해 새 객체를 반환하므로, 도메인 로직이 어디서 변경되는지 추적이 명확해짐
- 사이드 이펙트가 줄어들어 도메인 로직 단위 테스트가 쉬워짐

**Lombok 제거**
- Lombok은 컴파일 타임 코드 생성으로 IDE 지원이 불완전하고, Kotlin과 혼용 시 충돌 가능성이 있음
- Kotlin `data class`가 `equals()`, `hashCode()`, `toString()`, `copy()`를 언어 수준에서 제공하므로 Lombok 불필요

---

## 3. 단일 모듈 → 멀티 모듈 전환

### 결정
단일 Gradle 모듈에서 피처별 멀티 Gradle 모듈로 전환.

### 이유
단일 모듈에서는 패키지 규칙으로만 레이어 경계를 강제할 수 있다. 즉, 개발자가 실수로 도메인 패키지에서 JPA 어노테이션을 import해도 빌드가 통과한다.

멀티 모듈에서는 **Gradle 의존성 선언이 없으면 컴파일 자체가 불가능**하다. 예를 들어 `product:model` 모듈에 `spring-data-jpa` 의존성이 없으면 `@Entity`를 import하려는 순간 컴파일 에러가 발생한다.

**헥사고널 아키텍처와의 관계**
- 헥사고널의 핵심은 도메인이 외부(프레임워크, DB 등)에 의존하지 않는 것
- 멀티 모듈은 이 원칙을 빌드 툴 수준에서 물리적으로 강제함
- "규칙을 지키자"가 아니라 "규칙을 어기면 빌드가 실패한다"는 구조

---

## 4. 기능별 모듈 분리 (Feature-First)

### 결정
처음에는 레이어별로 모듈을 나누었다가(`product-domain`, `product-application`, `product-adapter-web`, `product-adapter-persistence`) 피처별로 모듈을 나누는 구조로 변경.

### 잘못된 방향 (레이어 우선 분리)
```
product-domain/
product-application/
product-adapter-web/
product-adapter-persistence/
cart-domain/
...
```

이 구조는 피처 하나를 보려면 모듈 4개를 넘나들어야 하고, 피처 간 경계보다 레이어 경계가 더 강하게 드러난다.

### 올바른 방향 (피처 우선 분리)
```
product/
  model/
  infrastructure/
  service/
  repository-jpa/
  api/
  schema/
cart/
  ...
```

피처(`product`, `cart`, `user`)가 최상위 경계가 되고, 그 안에서 레이어를 서브 모듈로 나눈다.

### 이유
**응집도(Cohesion)**
- 상품과 관련된 모든 것(도메인, 서비스, API, DB)이 `product/` 디렉토리 안에 있음
- 상품 기능을 추가하거나 변경할 때 `product/` 안에서만 작업이 완결됨

**헥사고널 아키텍처와의 관계**
- 헥사고널은 원래 피처(bounded context) 중심의 설계
- 레이어가 아닌 피처를 경계로 삼아야 한 피처의 변경이 다른 피처로 전파되지 않음
- `cart`가 `product`를 모르면, `product`의 내부 구조가 어떻게 바뀌든 `cart`는 영향을 받지 않음

---

## 5. 단일 피처 모듈 → 서브 모듈 분리

### 결정 과정
처음에는 피처를 단일 Gradle 모듈로 잡고 레이어를 패키지로만 구분했다.

```
product/  ← Gradle 모듈 하나
  └── src/main/kotlin/.../product/
      ├── domain/
      ├── usecase/
      ├── jpa/
      ├── adapter/
      └── api/
```

이후 서브 모듈로 변경하여 레이어 경계를 빌드 수준에서 강제.

```
product/
  model/          ← Gradle 서브 모듈
  infrastructure/ ← Gradle 서브 모듈
  service/        ← Gradle 서브 모듈
  repository-jpa/ ← Gradle 서브 모듈
  api/            ← Gradle 서브 모듈
  schema/         ← Gradle 서브 모듈
```

### 이유
패키지 수준 분리와 서브 모듈 분리의 차이:

| 구분 | 패키지 분리 | 서브 모듈 분리 |
|------|------------|--------------|
| 경계 강제 | 규칙(convention) | 빌드(compile-time) |
| 위반 감지 | 코드 리뷰에서 발견 | 빌드 실패로 즉시 발견 |
| 도메인 순수성 | 실수로 오염 가능 | 물리적으로 차단 |

`product:model`에 `spring-boot-starter-data-jpa` 의존성이 없으면 `Product.kt`에 `@Entity`를 쓰는 것 자체가 불가능하다. 이것이 서브 모듈 분리의 핵심 가치다.

### 각 서브 모듈의 의존성 규칙

| 서브 모듈 | 허용 의존성 | 금지 의존성 |
|----------|-----------|-----------|
| `model` | `shared`, 순수 Kotlin | Spring, JPA 전부 |
| `infrastructure` | `model`, `shared` | Spring, JPA |
| `service` | `model`, `infrastructure`, `shared`, Spring-context/tx | Spring-web, JPA |
| `repository-jpa` | `model`, `infrastructure`, `shared`, Spring-data-jpa | Spring-web |
| `api` | `model`, `service`, `shared`, Spring-web | JPA |
| `schema` | 없음 (SQL 리소스만) | 전부 |

---

## 6. Classpath와 서브 모듈의 동작 원리

### Classpath란
JVM이 클래스를 찾는 경로다. 컴파일러와 런타임 모두 classpath에 있는 `.class` 파일만 인식할 수 있다. classpath에 없는 클래스는 import 자체가 불가능하다.

### 단일 모듈의 classpath

`build.gradle.kts`가 하나이므로 거기 선언된 모든 의존성이 **전체 소스 코드**에 적용된다.

```kotlin
// 단일 모듈 build.gradle.kts
dependencies {
    implementation("spring-boot-starter-data-jpa")
    implementation("spring-boot-starter-web")
    implementation("spring-kafka")
}
```

빌드 결과물이 하나의 디렉토리에 모인다:

```
project/build/classes/kotlin/main/
  com/eventpurchase/project/product/Product.class      ← 도메인
  com/eventpurchase/project/product/ProductEntity.class ← JPA
  com/eventpurchase/project/cart/Cart.class
  com/eventpurchase/project/cart/CartEntity.class
  com/eventpurchase/project/user/User.class
```

`Product.kt`를 컴파일할 때 classpath에 이미 `spring-data-jpa`가 있으므로 `@Entity`를 import해도 컴파일러는 아무 에러도 내지 않는다. 막는 것은 개발자의 규칙뿐이다.

### 서브 모듈의 classpath

`build.gradle.kts`가 모듈마다 따로 있어서 각 모듈이 독립된 classpath를 가진다.

```kotlin
// product:model/build.gradle.kts
dependencies {
    implementation(project(":shared"))
    // JPA 없음, Spring 없음 → classpath에 해당 jar 자체가 없음
}

// product:repository-jpa/build.gradle.kts
dependencies {
    implementation(project(":product:model"))
    implementation(project(":product:infrastructure"))
    implementation("spring-boot-starter-data-jpa")
    // JPA는 여기만 존재
}
```

빌드 결과물이 모듈별로 분리된다:

```
product/model/build/classes/kotlin/main/
  com/eventpurchase/project/product/Product.class       ← 도메인만

product/repository-jpa/build/classes/kotlin/main/
  com/eventpurchase/project/product/ProductEntity.class ← JPA만

cart/model/build/classes/kotlin/main/
  com/eventpurchase/project/cart/Cart.class             ← 도메인만
```

`product:model`을 컴파일할 때 classpath는 `shared`와 Kotlin stdlib뿐이다. `jakarta.persistence.Entity` 클래스 자체가 classpath에 없으므로 `@Entity`를 쓰는 순간 **"Unresolved reference: Entity"** 컴파일 에러가 발생한다.

### settings.gradle.kts와 build.gradle.kts의 역할 분리

```kotlin
// settings.gradle.kts → 어떤 모듈이 존재하는지 Gradle에게 알림
include(
    "product:model",
    "product:repository-jpa",
    "cart:model",
    ...
)
```

```kotlin
// 각 모듈의 build.gradle.kts → 해당 모듈의 classpath(의존성) 정의
// Gradle이 include()로 모듈을 인식한 후
// 각 디렉토리의 build.gradle.kts를 읽어 classpath를 구성
```

두 파일의 관계:
```
settings.gradle.kts  →  모듈 목록 선언 (어떤 모듈이 있는가)
build.gradle.kts     →  모듈별 의존성 선언 (무엇을 알 수 있는가)
Gradle               →  둘을 조합해서 모듈별 컴파일 classpath 구성
```

### Gradle의 의존성 처리 과정

`implementation(project(":product:model"))`을 선언하면 Gradle이:

1. `product:model`을 먼저 컴파일
2. 그 결과물 경로(`product/model/build/classes/`)를 현재 모듈의 컴파일 classpath에 추가
3. 현재 모듈 컴파일 시작

의존성 트리를 직접 확인하려면:
```bash
./gradlew :product:model:dependencies
./gradlew :product:repository-jpa:dependencies
./gradlew :application-api:dependencies
```

---

## 7. 빌드 타임 vs 런타임 vs 테스트

### 빌드 타임 (컴파일)
모듈별로 classpath가 분리된 상태에서 각각 컴파일된다. 의존성 경계가 물리적으로 강제되는 시점이다.

```
product:model 컴파일      → classpath: shared 만
product:service 컴파일    → classpath: model + infrastructure + spring-tx
product:repository-jpa 컴파일 → classpath: model + infrastructure + spring-data-jpa
```

### 런타임 (운영 배포)

`application-api/build.gradle.kts`가 모든 모듈을 의존성으로 선언한다:

```kotlin
dependencies {
    implementation(project(":product:api"))
    implementation(project(":product:repository-jpa"))
    runtimeOnly(project(":product:schema"))
    implementation(project(":cart:api"))
    implementation(project(":cart:repository-jpa"))
    runtimeOnly(project(":cart:schema"))
    implementation(project(":user:repository-jpa"))
    runtimeOnly(project(":user:schema"))
}
```

`bootJar` 태스크가 모든 모듈의 `classes/`를 하나의 실행 가능한 jar로 합친다:

```bash
./gradlew :application-api:bootJar

# 결과: application-api/build/libs/application-api.jar
# 내부에 모든 서브 모듈의 클래스가 포함됨
```

런타임 classpath는 모든 모듈의 합집합:

```
런타임 classpath = [
  product/model/build/classes/,
  product/service/build/classes/,
  product/repository-jpa/build/classes/,
  cart/model/build/classes/,
  cart/repository-jpa/build/classes/,
  user/repository-jpa/build/classes/,
  spring-boot.jar, spring-data-jpa.jar, postgresql.jar, ...
]
```

`ProjectApplication`이 실행되면 Spring이 이 전체 classpath를 스캔해서 `@Service`, `@Component`, `@RestController` 빈을 모두 등록한다. 서브 모듈 분리는 런타임에는 의미가 없고, 하나의 JVM 프로세스로 동작한다.

```
빌드 타임  →  모듈별 classpath 분리  →  의존성 위반 즉시 컴파일 에러
런타임     →  단일 jar, 단일 JVM     →  Spring이 전체 스캔해서 조립
```

### 테스트

테스트는 어떤 어노테이션을 쓰느냐에 따라 로드하는 범위가 달라진다.

**단위 테스트** — Spring 컨텍스트 없음
```kotlin
// product:service 모듈에서 단독 실행 가능
// Spring, JPA가 classpath에 없으므로 Mock 없이는 불가 → 강제로 순수 테스트
class ProductServiceTest {
    private val productRepository = mockk<ProductRepository>()
    private val productService = ProductServiceImpl(productRepository)

    @Test
    fun `상품이 없으면 예외를 던진다`() {
        every { productRepository.findById(1L) } returns null
        assertThrows<CustomException> { productService.getProductById(1L) }
    }
}
```

Spring 컨텍스트 로딩 없이 JVM만 뜨므로 테스트 속도가 압도적으로 빠르다.

**슬라이스 테스트** — 해당 레이어 빈만 로드
```kotlin
// product:repository-jpa 모듈 통합 테스트
@DataJpaTest
@Import(ProductPersistenceAdapter::class)
class ProductPersistenceAdapterTest {
    // product:repository-jpa의 classpath만 있으므로
    // CartEntity, UserEntity 스캔 대상 아님
    // 단일 모듈이었다면 전부 스캔되어 설정 충돌 가능
}
```

```kotlin
// product:api 모듈 테스트
@WebMvcTest(ProductController::class)
class ProductControllerTest {
    @MockkBean lateinit var productService: ProductService
    // JPA 빈 없음, 웹 레이어만 로드
}
```

**통합 테스트** — 전체 컨텍스트
```kotlin
// application-api 모듈에서만 의미 있음
@SpringBootTest
class ProductIntegrationTest {
    // 모든 모듈의 빈을 전부 로드
    // 운영 환경과 동일한 조건에서 테스트
}
```

단일 모듈이었다면 `@DataJpaTest` 시 `CartEntity`, `UserEntity`가 같은 classpath에 있어 전부 스캔 대상이 되고, 테스트와 무관한 빈 설정 충돌이 발생할 수 있다. 서브 모듈 분리는 **테스트 격리와 속도**를 동시에 해결한다.

### 포트 의존과 구현체 교체 시 테스트 수정 여부

**단일 모듈 - 계층 간 직접 의존**

서비스가 구현체를 직접 의존하면 테스트도 그 구현체를 직접 의존해야 한다.

```kotlin
// 서비스가 JPA 구현체 직접 의존
class StockServiceImpl(
    private val stockJpaRepository: StockJpaRepository
) {
    fun decrease(productId: Long, quantity: Int) {
        val stock = stockJpaRepository.findById(productId)
        ...
    }
}

// 테스트도 JPA 구현체 직접 의존
class StockServiceTest {
    private val stockJpaRepository = mockk<StockJpaRepository>()
    private val stockService = StockServiceImpl(stockJpaRepository)
}
```

JPA → Redis로 교체하면:

```kotlin
// 서비스 수정
class StockServiceImpl(
    private val redisTemplate: RedisTemplate<String, Int>  // 교체
)

// 테스트도 수정 불가피
class StockServiceTest {
    private val redisTemplate = mockk<RedisTemplate<String, Int>>()  // 교체
    private val stockService = StockServiceImpl(redisTemplate)
}
```

비즈니스 로직은 그대로인데 구현이 바뀌었다는 이유만으로 서비스 코드와 테스트 코드를 둘 다 수정해야 한다.

---

**서브 모듈 - 포트에만 의존**

서비스가 포트(인터페이스)에만 의존하면 구현체가 무엇이든 상관없다.

```kotlin
// 포트 정의
interface StockPort {
    fun getStock(productId: Long): Int
    fun decrease(productId: Long, quantity: Int)
}

// 서비스는 포트만 앎
class StockServiceImpl(
    private val stockPort: StockPort  // 구현 모름
) {
    fun decrease(productId: Long, quantity: Int) {
        val current = stockPort.getStock(productId)
        if (current < quantity) throw CustomException(ErrorCode.OUT_OF_STOCK)
        stockPort.decrease(productId, quantity)
    }
}

// 테스트도 포트만 mock
class StockServiceTest {
    private val stockPort = mockk<StockPort>()
    private val stockService = StockServiceImpl(stockPort)

    @Test
    fun `재고 부족하면 예외`() {
        every { stockPort.getStock(1L) } returns 5
        assertThrows<CustomException> { stockService.decrease(1L, 10) }
    }
}
```

JPA → Redis로 교체해도:

```kotlin
// StockServiceImpl → 안 바뀜
// StockServiceTest → 안 바뀜

// 출력 어댑터만 교체
class StockJpaAdapter : StockPort { ... }   // 기존
class StockRedisAdapter : StockPort { ... } // 교체
```

`StockPort`를 구현하는 어댑터만 바꾸면 된다. 서비스도, 테스트도 포트에만 의존하고 있어서 구현체가 무엇인지 모른다.

각 서브 모듈은 자기 테스트를 독립적으로 가진다:

```
inventory:service 테스트        → StockPort mock, 비즈니스 로직만 검증, 항상 통과
inventory:repository-jpa 테스트 → JPA 어댑터가 StockPort 올바르게 구현하는지 검증
inventory:cache-redis 테스트    → Redis 어댑터가 StockPort 올바르게 구현하는지 검증
```

`application-api`에서 어떤 구현을 선택하든 각 모듈 테스트는 독립적으로 돌아간다. `application-api` 수정은 운영에서 어떤 구현을 쓸지 선택하는 것이고 테스트와는 별개다.

---

**서브 모듈이 포트 경계를 강제하는 이유**

단일 모듈은 같은 classpath에 JPA, Redis가 전부 있어서 서비스에서 포트를 우회하고 구현체를 직접 쓸 수 있다. 규칙으로만 막는 것이다.

```kotlin
// 단일 모듈 - 컴파일 통과, 포트 우회 가능
class StockServiceImpl(
    private val stockPort: StockPort,
    private val stockJpaRepository: StockJpaRepository  // 실수로 직접 주입
) {
    fun decrease(productId: Long, quantity: Int) {
        stockJpaRepository.findById(productId)  // 포트 안 쓰고 직접 호출
    }
}
```

서브 모듈은 `inventory:service` classpath에 JPA가 없으니 물리적으로 불가능하다.

```kotlin
// 서브 모듈 - 컴파일 에러
class StockServiceImpl(
    private val stockJpaRepository: StockJpaRepository  // Unresolved reference
)
```

```
단일 모듈  →  구현체 직접 의존 가능  →  구현 바뀌면 서비스/테스트 수정 불가피
서브 모듈  →  포트에만 의존 강제     →  구현체만 교체, 서비스/테스트 수정 없음
```

---

## 8. 인프라 의존성 격리 확장 (Redis, Kafka)

서브 모듈의 의존성 격리는 JPA에만 국한되지 않는다. Redis, Kafka 등 어떤 인프라든 동일한 원칙으로 서브 모듈을 추가하면 된다.

### Kafka 이벤트 발행 추가 예시

```kotlin
// cart:infrastructure
// cart가 정의한 아웃바운드 포트 - Kafka 존재 모름
interface CartEventPort {
    fun publish(event: CartItemAddedEvent)
}
```

```kotlin
// cart:event-kafka/build.gradle.kts
dependencies {
    implementation(project(":cart:infrastructure"))  // 포트 인터페이스만
    implementation("org.springframework.kafka:spring-kafka")  // Kafka는 여기만
}

// cart:event-kafka
@Component
internal class CartKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>
) : CartEventPort {
    override fun publish(event: CartItemAddedEvent) {
        kafkaTemplate.send("cart.item.added", event.toJson())
    }
}
```

`cart:model`, `cart:service`는 Kafka의 존재를 전혀 모른다. `CartEventPort`라는 인터페이스만 알고, 실제 구현이 Kafka인지 Redis Pub/Sub인지 RabbitMQ인지는 해당 어댑터 모듈만 안다.

### Redis 캐시 추가 예시

```kotlin
// product:cache-redis/build.gradle.kts
dependencies {
    implementation(project(":product:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Redis는 여기만 존재
}

// product:repository-jpa/build.gradle.kts
dependencies {
    // Redis 없음, JPA만
}
```

```
product:model          → Redis 모름, JPA 모름
product:service        → Redis 모름, JPA 모름
product:cache-redis    → Redis만 앎
product:repository-jpa → JPA만 앎
```

### 어댑터 교체의 실질적 의미

나중에 Redis를 Memcached로 바꾸고 싶다면 `product:cache-redis` 모듈만 `product:cache-memcached`로 교체하면 된다. `product:model`, `product:service`, `product:api`는 손댈 필요가 없다. 인터페이스가 바뀌지 않으니 컴파일 에러도 없다.

이것이 헥사고널의 **"어댑터는 교체 가능해야 한다"** 원칙이 실제 코드 구조로 구현되는 방식이다. 서브 모듈이 없으면 이 원칙은 설계 문서에만 존재하고, 서브 모듈이 있으면 물리적으로 강제된다.

---

## 9. CartItem의 Product 의존 제거

### 기존 문제
```kotlin
// 기존: CartItem이 Product 엔티티를 직접 참조
class CartItemEntity {
    @ManyToOne
    val product: ProductEntity  // product 모듈에 JPA 수준으로 결합
}
```

이 구조에서는 `ProductEntity`의 컬럼이 바뀌면 `CartItemEntity`도 영향을 받는다. 피처 간 경계가 없는 것과 같다.

### 해결
`CartItem`은 `productId: Long`만 저장하고, cart가 필요한 상품 정보는 cart가 직접 정의한 `ProductSummary`라는 자체 뷰 모델로 표현한다.

```
cart:model
  └── ProductSummary.kt  ← product 모듈 타입 없음, cart가 자체 정의
  └── CartItem.kt        ← productId: Long만 저장

cart:infrastructure
  └── ProductQueryPort.kt  ← cart가 정의한 아웃바운드 포트

cart:repository-jpa
  └── ProductQueryAdapter.kt  ← product:infrastructure의 ProductRepository를 사용해 구현
```

### 경계 검증
- `cart:model` → product 모듈 import 없음 ✓
- `cart:infrastructure` → product 모듈 import 없음 ✓
- `cart:service` → product 모듈 import 없음 ✓
- `cart:repository-jpa` → `product:infrastructure`(인터페이스)만 사용 ✓
- `CartItemEntity` → `productId: Long` 컬럼만 저장, `@ManyToOne ProductEntity` 없음 ✓

### 헥사고널 아키텍처와의 관계
이것이 헥사고널의 **Bounded Context 격리**다. `cart` 도메인은 `product` 도메인의 내부 구현을 전혀 모른다. `cart`가 필요한 상품 정보의 형태(`ProductSummary`)를 스스로 정의하고, 그것을 어떻게 가져올지는 `ProductQueryPort`라는 포트(인터페이스)로 추상화한다. 실제 구현(`ProductQueryAdapter`)은 가장 바깥 레이어인 `repository-jpa`에만 존재한다.

---

## 10. UseCase 인터페이스 통합

### 기존 구조 (과분리)
```
product/usecase/GetProductsUseCase.kt
product/usecase/GetProductUseCase.kt
cart/usecase/GetCartUseCase.kt
cart/usecase/AddCartItemUseCase.kt
cart/usecase/UpdateCartItemUseCase.kt
cart/usecase/DeleteCartItemUseCase.kt
cart/usecase/ClearCartUseCase.kt
```

### 변경 후
```
product/service/ProductService.kt  ← 인터페이스 + 구현 한 파일
cart/service/CartService.kt        ← 인터페이스 + 구현 한 파일
```

```kotlin
interface ProductService {
    fun getProducts(lastId: Long?, size: Int): CursorResponse<Product>
    fun getProductById(id: Long): Product
}

interface CartService {
    fun getCart(userId: Long): CartView
    fun addItem(userId: Long, productId: Long, quantity: Int): CartView
    fun updateItemQuantity(userId: Long, cartItemId: Long, quantity: Int): CartView
    fun deleteItem(userId: Long, cartItemId: Long): CartView
    fun clearCart(userId: Long)
}
```

### 이유
UseCase를 기능 하나당 파일 하나로 쪼개는 것은 **ISP(인터페이스 분리 원칙)**를 과도하게 적용한 것이다. 이 프로젝트에서는 "이 피처가 외부에 어떤 기능을 제공하는가"를 한눈에 보는 것이 더 중요하다. 인터페이스 하나를 열면 해당 피처의 전체 능력(capability)이 보여야 한다.

인터페이스 구현체는 `internal class`로 선언하여 서브 모듈 외부에서는 접근 불가하게 하고, 외부는 인터페이스만 사용한다.

---

## 11. DTO 위치 결정 (api 레이어)

### 결정
응답/요청 DTO는 `api` 서브 모듈 안에 둔다.

```
product/api/src/main/kotlin/.../product/
  └── ProductController.kt
  └── ProductResponse.kt   ← 같은 파일 or 같은 패키지
```

### 이유
DTO는 HTTP 요청/응답을 직렬화(JSON → 객체)하고 역직렬화(객체 → JSON)하는 역할이다. 이것은 전적으로 API 레이어의 관심사이다. 도메인 객체(`Product`)가 JSON 직렬화 방식을 알 필요가 없다.

도메인 → DTO 변환은 `controller` 파일 내 `private fun Domain.toResponse()` 확장 함수로 처리한다. 이렇게 하면 변환 로직이 API 레이어에 국한되고, 도메인 객체에 `@JsonProperty` 같은 외부 프레임워크 어노테이션이 침투하지 않는다.

### 헥사고널 아키텍처와의 관계
헥사고널에서 `api` 레이어는 **인바운드 어댑터**다. 외부(HTTP)의 형식을 내부(도메인)의 형식으로 변환하는 책임을 가진다. DTO는 이 변환의 산물이므로 어댑터 레이어에 있는 것이 맞다.

---

## 12. Schema = SQL 마이그레이션

### 결정
`schema` 서브 모듈은 Kotlin 소스 없이 SQL 마이그레이션 파일만 포함한다.

```
product/schema/
  └── src/main/resources/db/migration/
      └── V1__create_products.sql
```

### 이유
처음에는 `schema/`를 DTO나 응답 구조를 담는 곳으로 잘못 사용했다. 올바른 의미는 참고 프로젝트(`hexagonal-module-sample`)에서 확인했듯이 **DB 테이블 정의, 제약 조건, 인덱스, 마이그레이션 스크립트**를 담는 곳이다.

각 피처가 자신의 테이블 스키마를 소유함으로써:
- `ProductEntity`의 컬럼이 바뀌면 `product/schema/V2__add_column_products.sql`이 같은 피처 안에 함께 존재
- 코드 변경과 스키마 변경이 같은 PR에서 추적 가능
- 어느 피처가 어떤 테이블을 소유하는지 명확

---

## 13. SQL 마이그레이션 위치

### 논의
SQL 파일을 각 피처 모듈의 `resources/`에 두는 것과 `application-api/resources/`에 모으는 것을 검토했다.

**각 피처 모듈 resources (채택)**
- 피처가 자신의 스키마를 소유 → 응집도 높음
- 관련 코드(Entity)와 스키마(SQL)가 같은 모듈에 위치 → 추적 용이
- Flyway는 classpath를 스캔하므로 `application-api`가 각 모듈에 의존하면 모든 SQL이 자동 수집됨

**application-api resources**
- 마이그레이션 순서(V1, V2, V3)를 한 곳에서 관리
- 전체 DB 스키마를 한눈에 볼 수 있음
- 그러나 스키마 변경이 피처 모듈과 분리되어 추적이 어려워짐

### 결론
각 피처 모듈에 두는 것이 피처 응집도 측면에서 더 올바르다. "마이그레이션 파일이 흩어진다"는 것은 단점이 아니라 오히려 피처별 소유권이 명확한 것이다.

---

## 14. 모듈러 모놀리스 vs MSA

### 이 구조는 MSA가 아니다
현재 구조는 **모듈러 모놀리스(Modular Monolith)**다.

| 항목 | 현재 구조 | MSA |
|------|----------|-----|
| 배포 단위 | `application-api` 하나 | product 서버, cart 서버, user 서버 각각 |
| 프로세스 | 단일 | 서비스별 독립 프로세스 |
| 통신 방식 | 메서드 직접 호출 | REST, gRPC, 메시지 큐 |
| DB | 하나의 PostgreSQL | 서비스별 독립 DB 권장 |

### MSA 전환 가능성
현재 구조의 핵심 가치 중 하나는 **MSA 전환 용이성**이다. `product`, `cart`, `user` 간 경계가 코드 수준에서 명확하게 유지되므로, 필요 시 각 피처를 독립 Spring Boot 애플리케이션으로 분리할 수 있다. 메서드 호출을 REST/Kafka로 교체하는 것이 경계가 없는 모놀리스보다 훨씬 쉽다.

---

## 15. 최종 모듈 구조

```
project/
├── shared/                         # 공통 예외, 응답 래퍼 (Spring 의존 없음)
│
├── product/
│   ├── model/                      # Product.kt (순수 Kotlin, Spring 의존 없음)
│   ├── infrastructure/             # ProductRepository 인터페이스 (아웃바운드 포트)
│   ├── service/                    # ProductService 인터페이스 + internal 구현체
│   ├── repository-jpa/             # ProductEntity, JPA 구현체, PersistenceAdapter
│   ├── api/                        # ProductController, ProductResponse DTO
│   └── schema/                     # V1__create_products.sql
│
├── cart/
│   ├── model/                      # Cart, CartItem, CartView, ProductSummary (Spring 없음)
│   ├── infrastructure/             # CartRepository, CartItemRepository,
│   │                               # ProductQueryPort, UserValidationPort (아웃바운드 포트)
│   ├── service/                    # CartService 인터페이스 + internal 구현체
│   ├── repository-jpa/             # CartEntity, CartItemEntity(productId:Long),
│   │                               # ProductQueryAdapter, UserValidationAdapter
│   ├── api/                        # CartController, CartRequest/Response DTO
│   └── schema/                     # V3__create_carts.sql
│
├── user/
│   ├── model/                      # User.kt (순수 Kotlin)
│   ├── infrastructure/             # UserRepository 인터페이스
│   ├── repository-jpa/             # UserEntity, JPA 구현체
│   └── schema/                     # V2__create_users.sql
│
└── application-api/                # ProjectApplication, GlobalExceptionHandler
                                    # 모든 서브 모듈을 조립하는 진입점
```

---

## 16. 의존성 흐름 요약

### 헥사고널 의존성 방향 원칙
```
api(어댑터) → service(포트/인) → infrastructure(포트/아웃) ← repository-jpa(어댑터)
                    ↓
                  model(도메인)
```

도메인(`model`)은 아무것도 의존하지 않는다. 모든 의존성이 도메인을 향한다.

### Cross-Feature 의존성 (cart → product/user)

```
cart:model          ─── product 모름 ───────────────────────────── ✓
cart:infrastructure ─── product 모름 ───────────────────────────── ✓
cart:service        ─── product 모름 (ProductQueryPort만 앎) ────── ✓
cart:repository-jpa ─── product:infrastructure 의존 (인터페이스만) ✓
                    └── user:infrastructure 의존 (인터페이스만) ─── ✓
```

`cart`의 도메인과 서비스는 `product`나 `user`를 전혀 모른다. 크로스 피처 의존은 가장 바깥 레이어(`repository-jpa`)에서만 발생하며, 그것도 구현체가 아닌 **인터페이스**에만 의존한다.

### application-api 의존성
```
application-api
  ├── product:model        (runtimeOnly - JVM 클래스 로딩 보장)
  ├── product:infrastructure (runtimeOnly - JVM 클래스 로딩 보장)
  ├── product:api          (컨트롤러)
  ├── product:repository-jpa (JPA 빈)
  ├── product:schema       (SQL 리소스, runtimeOnly)
  ├── cart:model           (runtimeOnly)
  ├── cart:infrastructure  (runtimeOnly)
  ├── cart:api
  ├── cart:repository-jpa
  ├── cart:schema
  ├── user:model           (runtimeOnly)
  ├── user:infrastructure  (runtimeOnly)
  ├── user:repository-jpa
  └── user:schema
```

`application-api`는 코드를 직접 작성하지 않고 모든 모듈을 조립하는 역할만 한다. Spring Boot가 여기서 뜨면 컴포넌트 스캔으로 각 모듈의 `@Service`, `@Component`, `@RestController` 빈을 모두 수집한다.

---

## 14. IntelliJ 런타임 클래스패스 전파 문제

### 문제 상황

앱 실행 시 아래 에러가 발생했다:

```
Caused by: java.lang.ClassNotFoundException: com.eventpurchase.project.user.User
    at java.base/java.lang.Class.getDeclaredMethods0(Native Method)
    at org.hibernate.boot.model.internal.EntityBinder.bindEntityClass(...)
```

`User.class`는 `user:model` 모듈에 존재하고, 실제로 jar에도 컴파일 결과에도 정상적으로 있었다. 그런데도 런타임에 찾지 못했다.

### 원인 - Gradle `implementation`과 IntelliJ의 처리 방식 차이

Gradle에서 의존성 scope:

```
api(project(":user:model"))           → 이 모듈을 쓰는 쪽에도 노출 (전이됨)
implementation(project(":user:model")) → 이 모듈 내부에서만 씀 (전이 안 됨)
```

`user:repository-jpa/build.gradle.kts`에서:

```kotlin
implementation(project(":user:model"))  // 외부에 숨김
```

Gradle `bootJar` / `bootRun`은 런타임 클래스패스를 계산할 때 `implementation`이어도 전이 의존성을 전부 포함한다. 문제가 없다.

그러나 **IntelliJ가 직접 실행**할 때는 다르다. IntelliJ는 Gradle을 임포트하면서 `implementation` 의존성을 `export=false` 모듈 의존성으로 매핑한다. `export=false`인 의존성은 소비자 모듈에 전파되지 않는다.

```
Gradle bootRun 관점:
application-api → user:repository-jpa → user:model  ← 전부 포함 ✅

IntelliJ 직접 실행 관점:
application-api → user:repository-jpa (export=false) → user:model  ← 전파 안 됨 ❌
```

### 왜 그 시점에 에러가 터졌냐

Hibernate는 앱 시작 시 `@Entity` 클래스를 찾아 메타데이터를 구성한다. `UserEntity`를 처리하면서 `getDeclaredMethods()`를 호출하는 순간, JVM이 메서드 시그니처를 해석하기 위해 반환 타입인 `User.class`를 로드하려 한다.

```kotlin
// UserEntity.kt
fun toDomain() = User(...)  // 반환 타입 User → JVM이 User.class 필요
```

이건 헥사고널 아키텍처 이야기가 아니다. **JVM이 어떤 클래스를 메모리에 올릴 때, 그 클래스가 참조하는 모든 타입의 `.class` 파일이 클래스패스에 있어야 한다는 JVM 기본 원칙**이다.

같은 이유로 `user:infrastructure`도 추가했다:

```kotlin
// UserPersistenceAdapter.kt
internal class UserPersistenceAdapter : UserRepository  // UserRepository.class 필요
```

`UserPersistenceAdapter`가 `UserRepository`를 구현하므로, JVM이 `UserPersistenceAdapter.class`를 올릴 때 `UserRepository.class`도 있어야 한다. 역시 IntelliJ가 `user:infrastructure`를 전파하지 않으므로 명시적으로 추가해야 한다.

### 해결

`application-api/build.gradle.kts`에 명시적으로 추가:

```kotlin
// user
runtimeOnly(project(":user:model"))          // User.class 보장
runtimeOnly(project(":user:infrastructure")) // UserRepository.class 보장
implementation(project(":user:repository-jpa"))
```

`runtimeOnly`를 쓴 이유: `application-api` 코드에서 `User`나 `UserRepository`를 직접 import해서 쓰지는 않는다. JVM이 클래스를 올릴 때만 필요하므로 런타임 전용이 의미상 정확하다.

### 근본 해결책

IntelliJ Settings → Build, Execution, Deployment → Build Tools → Gradle → **"Build and run using"을 `Gradle`로 변경**하면 Gradle이 직접 클래스패스를 구성하므로 이 문제 자체가 발생하지 않는다.
