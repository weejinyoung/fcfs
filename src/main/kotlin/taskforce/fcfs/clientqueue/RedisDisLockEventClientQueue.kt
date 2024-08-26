package taskforce.fcfs.clientqueue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult
import taskforce.fcfs.config.RedissonLockManager

@Component
@Deprecated(message = "Use RedisLuaEventClientQueue instead", ReplaceWith("RedisLuaEventClientQueue"))
class RedisDisLockEventClientQueue(
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

    override fun admitNextClients(request: Long) {
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

}
