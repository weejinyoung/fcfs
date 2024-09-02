package taskforce.fcfs.allocate

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import taskforce.fcfs.clientqueue.EventClientQueue
import taskforce.fcfs.config.RedissonLockManager
import taskforce.fcfs.util.scheduleWithFixedRate
import java.time.Duration

@Service
class FirstComeFirstServedEventService(
    private val eventClientQueue: EventClientQueue<String>,
    private val queueAdmitProperties: QueueAdmitProperties,
    private val redissonLockManager: RedissonLockManager,
    private val scheduler: ThreadPoolTaskScheduler
) {

    private val logger = KotlinLogging.logger {  }

    //@Counted("client.join")
    fun joinClientQueue(client: String) =
        eventClientQueue.join(client)

    // TODO 상세한 트리거 설정
    // TODO Quartz 등 다양한 custom 제공하는 스케줄러 라이브러리 도입 고려, 하지만 Quartz 는 분산 스케줄링 정보를 RDB 에 저장해야함
    @PostConstruct
    private fun admitClientsByPollingForMultiWas() {
        // TODO waitTime 이 사실상 무한대이다
        scheduler.scheduleWithFixedRate(Duration.ofMillis(queueAdmitProperties.getAdmitDelay())) {
            redissonLockManager.tryLockAndRepeatWith(
                lockName = "SCHEDULE",
                waitTime = Long.MAX_VALUE /*스케줄러 싱글스레드는 무한 락 대기하게 해보자, 스레드풀이 스케줄러 용 만인지 궁금하긴 하다*/,
                leaseTime = queueAdmitProperties.getAdmitDelay() * 5,
                delayTime = queueAdmitProperties.getAdmitDelay()
            ) {
                logger.info { "im a worker, thread ID is ${Thread.currentThread().id}" }
                eventClientQueue.admitClients(queueAdmitProperties.getAdmitRequest())
            }
        }
    }


//    @PostConstruct
//    private fun admitClientsByPollingForMultiWas() {
//        scheduler.scheduleWithFixedRate(Duration.ofMillis(queueAdmitProperties.getAdmitDelay())) {
//            redissonLockManager.tryLockWith(
//                "SCHEDULE",
//                queueAdmitProperties.getAdmitDelay(),
//                queueAdmitProperties.getAdmitDelay()) {
//                eventClientQueue.admitClients(queueAdmitProperties.getAdmitRequest())
//            }
//        }
//    }



}