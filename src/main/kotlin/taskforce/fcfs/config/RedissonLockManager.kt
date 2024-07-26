package taskforce.fcfs.config

import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedissonLockManager(
    private val redissonClient: RedissonClient
) {

    /*@Value("\${spring.data.redis.redisson.wait-time}")*/
    /*lateinit var*/ private val waitTime: Long = 2000

    /*@Value("\${spring.data.redis.redisson.lease-time}")*/
    /*lateinit var*/ private val leaseTime: Long = 2000

    private val LOCK_PREFIX = "LOCK:"

    fun <R> tryLockWith(
        lockName: String,
        task: () -> R,
    ): R = tryLockWith(
        lockName = lockName,
        waitTime = waitTime,
        leaseTime = leaseTime,
        task = task
    )

    fun <R> tryLockWith(
        lockName: String,
        waitTime: Long,
        leaseTime: Long,
        task: () -> R,
    ): R {
        val rLock: RLock = redissonClient.getLock(LOCK_PREFIX + lockName) // Lock 호출
        val available: Boolean = rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS) // Lock 획득 시도
        if (!available) { // 획득 시도를 실패했을 경우 Exception 처리
            throw RedisLockTimeoutException()
        }
        try {
            return task() // 전달 받은 람다 실행
        } finally {
            if (rLock.isHeldByCurrentThread) { // 해당 스레드가 Lock을 소유 중인지 확인
                rLock.unlock() // Lock 반환
            } else { // 스레드가 Lock을 소유 중이지 않을 경우, Exception (leaseTime을 넘은 경우)
                throw RedisLockTimeoutException()
            }
        }
    }
}