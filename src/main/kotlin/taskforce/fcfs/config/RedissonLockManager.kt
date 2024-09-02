package taskforce.fcfs.config

import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.TimeUnit

@Service
class RedissonLockManager(
    private val redissonClient: RedissonClient
) {

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

    fun <R> tryLockAndRepeatWith(
        lockName: String,
        waitTime: Long,
        leaseTime: Long,
        delayTime: Long,
        task: () -> R,
    ): R {
        val rLock: RLock = redissonClient.getLock(LOCK_PREFIX + lockName) // Lock 호출
        val available: Boolean = rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS) // Lock 획득 시도
        if (!available) { // 획득 시도를 실패했을 경우 Exception 처리
            throw RedissonDisLockWaitTimeoutException()
        }
        try {
            var count = 0;
            while (true) {
                task()
                // TODO Delay 를 주는 다른 방법?
                Thread.sleep(delayTime)
                // TODO 언제 다시 갱신할 것인지?, 일단 5번에 한 번
                if(count >= 5) {
                    rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)
                    count = 0
                }
                else count++
            }
        } finally {
            if (rLock.isHeldByCurrentThread) { // 해당 스레드가 Lock을 소유 중인지 확인
                rLock.unlock() // Lock 반환
            } else { // 스레드가 Lock을 소유 중이지 않을 경우, Exception (leaseTime을 넘은 경우)
                throw RedissonDisLockLeaseTimeoutException()
            }
        }
    }

}