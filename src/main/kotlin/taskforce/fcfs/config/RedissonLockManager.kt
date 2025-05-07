package taskforce.fcfs.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.TimeUnit

@Service
class RedissonLockManager(
    private val redissonClient: RedissonClient
) {

    private val logger = KotlinLogging.logger {  }
    private val LOCK_PREFIX = "LOCK:"

    fun <R> tryLockWith(
        lockName: String,
        waitTime: Long,
        leaseTime: Long,
        task: () -> R,
    ): R {
        val rLock: RLock = redissonClient.getLock(LOCK_PREFIX + lockName) // Lock 호출
        val available: Boolean = rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS) // Lock 획득 시도
        if (!available) { // 획득 시도를 실패했을 경우 Exception 처리
            throw RedissonDisLockWaitTimeoutException()
        }
        try {
            return task() // 전달 받은 람다 실행
        } finally {
            if (rLock.isHeldByCurrentThread) { // 해당 스레드가 Lock을 소유 중인지 확인
                rLock.unlock() // Lock 반환
            } else { // 스레드가 Lock을 소유 중이지 않을 경우, Exception (leaseTime을 넘은 경우)
                throw RedissonDisLockLeaseTimeoutException()
            }
        }
    }

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

}