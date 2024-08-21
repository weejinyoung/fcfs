package taskforce.fcfs.clientqueue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult
import taskforce.fcfs.config.RedissonLockManager

@Component
@Deprecated(message = "Use EventClientRedisLuaQueue instead", ReplaceWith("EventClientRedisLuaQueue"))
class EventClientRedisDisLockQueue(
    private val redissonLockManager: RedissonLockManager,
    private val redissonClient: RedissonClient,
    private val eventProperties: EventProperties
) : EventClientQueue<String> {

    companion object {
        private const val WAITING_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:WAITING:QUEUE"
        private const val ADMITTED_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:ADMITTED:QUEUE"
        private const val EVENT_DONE_MESSAGE = "Sorry but, event is over"
        private const val NOT_YET_JOIN_MESSAGE = "You didn't join yet"
    }

    // TODO Waiting Queue 의 최대 제한 설정... 이건 레디스에서 아니면 애플리케이션에서?
    private val waitingQueue =
        redissonClient.getScoredSortedSet<String>("${eventProperties.getEventName()}$WAITING_QUEUE_REDIS_KEY_POSTFIX")
    private val admittedQueue =
        redissonClient.getSet<String>("${eventProperties.getEventName()}$ADMITTED_QUEUE_REDIS_KEY_POSTFIX")
    private val logger = KotlinLogging.logger {}

    override fun join(client: String): JoinResult {
        if (admittedQueue.size >= eventProperties.getEventLimit()) {
            return JoinResult.Fail(EVENT_DONE_MESSAGE)
        }
        return System.nanoTime().let {
            JoinResult.Success(waitingQueue.addAndGetRank(it.toDouble(), client).toLong(), it)
        }
    }

    override fun admitNextClientsForStandalone(request: Long) {
        val current = admittedQueue.size
        if (current >= eventProperties.getEventLimit()) {
            logger.info { "Event is over" }
            return
        }
        val admit = minOf((eventProperties.getEventLimit() - current), request)
        val admittedClients = waitingQueue.valueRange(0, (admit - 1).toInt()).ifEmpty {
            logger.info { "Waiting queue is empty" }
            return
        }
        waitingQueue.removeRangeByRank(0, (admit - 1).toInt())
        admittedQueue.addAll(admittedClients)
    }

    override fun admitNextClientsForDistributed(request: Long) {
        redissonLockManager.tryLockWith(eventProperties.getEventName()) {
            val current = admittedQueue.size
            if (current >= eventProperties.getEventLimit()) {
                logger.info { "Event is over" }
                return@tryLockWith
            }
            val admit = minOf((eventProperties.getEventLimit() - current), request)
            val admittedClients = waitingQueue.valueRange(0, (admit - 1).toInt()).ifEmpty {
                logger.info { "Waiting queue is empty" }
                return@tryLockWith
            }
            waitingQueue.removeRangeByRank(0, (admit - 1).toInt())
            admittedQueue.addAll(admittedClients)
        }
    }

    override fun getWaitingRank(client: String): RankResult =
        waitingQueue.rank(client)
            ?.let { RankResult.Success(it) }
            ?: RankResult.Fail(NOT_YET_JOIN_MESSAGE)

//    override fun join(client: String): JoinResult =
//        if (admittedQueue.size >= eventProperties.getEventLimit())
//            JoinResult.Fail(EVENT_DONE_MESSAGE)
//        else JoinResult.Success(
//            waitingQueue.addAndGetRank(System.currentTimeMillis().toDouble(), client),
//            LocalDateTime.now()
//        )

//    override fun join(client: String): JoinResult {
//        admittedQueue.run {
//            if (size >= eventProperties.getEventLimit()) return JoinResult.Fail(EVENT_IS_OVER)
//            if (contains(client)) return JoinResult.Fail(ALREADY_ADMITTED_CLIENT)
//        }
//        return JoinResult.Success(waitingQueue.addAndGetRank(System.currentTimeMillis().toDouble(), client))
//    }


//    private val COUNT_REDIS_KEY_POSTFIX = ":CLIENT:ADMITTED:COUNT"
//    private val QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:QUEUE"

//    override fun join(event: String, client: String): Int =
//        redissonClient.getScoredSortedSet<String>("$event$QUEUE_REDIS_KEY_POSTFIX")
//            .addAndGetRank(System.currentTimeMillis().toDouble(), client)
//
//    override fun admitNextClients(event: String, eventLimit: Int, request: Int): List<String> =
//        redissonLockManager.tryLockWith(event) {
//            redissonClient.run {
//                val countConnector = getAtomicLong("${event.uppercase()}$COUNT_REDIS_KEY_POSTFIX")
//                val queueConnector = getScoredSortedSet<String>("${event.uppercase()}$QUEUE_REDIS_KEY_POSTFIX")
//
//                val current = countConnector.get().toInt()
//                if (current >= eventLimit) throw AdmissionLimitExceededException()
//
//                val admit = minOf((eventLimit - current), request)
//                val admittedClients = queueConnector.valueRange(0, admit - 1).ifEmpty { throw EmptyQueueException() }
//
//                countConnector.addAndGet(queueConnector.removeRangeByRank(0, admit - 1).toLong())
//                admittedClients.toList()
//            }
//        }
}
