### 선착순 시스템 요구사항

1. 최소의 클라우드 비용으로 선착순 시스템을 만들어야한다.
2. 중복 응모를 막아야한다.
3. 결과는 바로 알려주지 않아도 된다.
4. 만약 500명 제한이라면 1등에서 500등까지 순위도 알 수 있어야한다.

---

### 주요문제

분산 환경에서 중복된 컨슘 작업이 발생하고, 만약 하나의 컨슈머를 선정한다고 해도 Failover 전략을 수립해야한다.
이것을 가능한 최소의 비용으로 해결해야한다. 

---

### 문제배경

1. 선착순 서비스를 가장 적은 비용으로 구현하는 것이 목표, 이미 사용이 결정된 Redis 를 적극적으로 사용해야함.
2. 하나의 스프링 부트 프로그램이 퍼블리셔와 컨슈머 역할을 모두 할 수 있게끔 구현해야함.
3. 하나의 스프링 부트 프로그램을 사용하고있는 여러 서버 중 하나의 서버만 컨슈머로 선정되어야함.

---
### 문제 해결
1. 모든 선착순 API 애플리케이션은 부팅 시 선착순 용 스레드 풀의 스레드가 락 소유를 시도, 락 소유에 성공한 스레드만이 컨슘 로직을 실행할 수 있게 구현.
2. RLock 의 waittime 은 MAX_VALUE, leasetime 은 컨슘 간격 시간의 일정 배수만큼 설정.
3. 락을 소유한 스레드가 특정 주기마다 락을 갱신하는 방법으로 페일오버 전략을 수립.

---
### Redisson RLock 분산 스케줄링, Failover 시퀀스 다이어그램
![분산 스케줄링](https://github.com/user-attachments/assets/6ac1b15a-64bf-402c-a8fc-b735b3771038)

---

### 핵심 코드

```kotlin
    /**
     * 분산 환경에서 선착순 서비스를 위한 락 관리 메서드.
     *
     * 이 메서드는 여러 애플리케이션 인스턴스 중 하나만 컨슈머 역할을 수행하도록 보장합니다.
     * Redisson의 분산 락과 PubSub 메커니즘을 활용하여 효율적인 락 관리와 Failover를 구현합니다.
     *
     * 동작 방식:
     * 1. 모든 애플리케이션 인스턴스는 시작 시 락 획득을 시도합니다.
     * 2. 락을 획득한 인스턴스만 컨슈머 작업을 수행합니다.
     * 3. 락을 보유한 인스턴스는 주기적으로 락을 갱신하여 작업 중 락 유지를 보장합니다.
     * 4. 락을 보유한 인스턴스가 비정상 종료되면 leaseTime 후 락이 자동 해제됩니다.
     * 5. 다른 인스턴스는 락 해제 이벤트를 구독하고 있다가 락이 해제되면 즉시 획득을 시도합니다.
     *
     * @param lockName 락 이름 (여러 애플리케이션에서 공유되는 고유 식별자)
     * @param waitTime 락 획득 대기 시간 (밀리초). Long.MAX_VALUE로 설정 시 무한 대기.
     * @param leaseTime 락 유지 시간 (밀리초). 이 시간 동안 갱신하지 않으면 락이 자동 해제됨.
     * @param delayTime 컨슈머 작업 사이의 지연 시간 (밀리초)
     * @param task 락 획득 후 주기적으로 실행할 작업
     */
    fun <R> tryLockAndRepeatWith(
        lockName: String,
        waitTime: Long,
        leaseTime: Long,
        delayTime: Long,
        task: () -> R,
    ) {
        val logger = KotlinLogging.logger {}
        val rLock: RLock = redissonClient.getLock(LOCK_PREFIX + lockName)
        val renewInterval = (leaseTime / 3).coerceAtLeast(100)

        while (!Thread.currentThread().isInterrupted) {
            try {
                // 락 획득 시도 (PubSub 메커니즘으로 락 해제 알림 받음)
                if (!rLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS)) {
                    logger.debug { "Failed to acquire lock, waiting for notification..." }
                    continue
                }

                logger.info { "Lock acquired, starting consumer task" }
                var lastRenewTime = System.currentTimeMillis()

                try {
                    // 작업 실행 및 락 갱신 루프
                    while (!Thread.currentThread().isInterrupted) {
                        task() // 컨슘 작업 실행
                        Thread.sleep(delayTime)

                        // 락 갱신 체크
                        if (System.currentTimeMillis() - lastRenewTime >= renewInterval) {
                            if (rLock.isHeldByCurrentThread && rLock.tryLock(0, leaseTime, TimeUnit.MILLISECONDS)) {
                                lastRenewTime = System.currentTimeMillis()
                            } else {
                                break // 락 갱신 실패 시 루프 종료
                            }
                        }
                    }
                } finally {
                    if (rLock.isHeldByCurrentThread) {
                        rLock.unlock()
                        logger.info { "Lock released" }
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                logger.error(e) { "Error occurred, retrying after 1 second" }
                Thread.sleep(1000)
            }
        }
    }

```
