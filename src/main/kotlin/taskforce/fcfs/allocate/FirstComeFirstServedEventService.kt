package taskforce.fcfs.allocate

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

    fun joinClientQueue(client: String) =
        eventClientQueue.join(client)

    @PostConstruct
    private fun schedule() {
        scheduler.scheduleWithFixedRate(Duration.ofMillis(queueAdmitProperties.getAdmitDelay())) {
            redissonLockManager.tryLockWith("SCHEDULE") {
                eventClientQueue.admitNextClients(queueAdmitProperties.getAdmitRequest())
            }
        }
    }

    // TODO Quartz 등 다양한 custom 제공하는 스케줄러 라이브러리 도입 고려
    // TODO Quartz 는 분산 스케줄링의 정보를 저장하는 데이터베이스를 관계형데이터베이스로 픽스했다는 소식을 들음..
//    @Scheduled(fixedDelay = 500)
//    private fun pollingClientQueue() =
//        eventClientQueue.admitNextClients(queueAdmitProperties.getAdmitRequest())

}