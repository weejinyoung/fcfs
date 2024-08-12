package taskforce.fcfs.clientqueue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult


@Component
class EventClientNonLockQueue(
    private val eventProperties: EventProperties,
    private val lettuceClient: RedisTemplate<String, String>
) : EventClientQueue<String> {

    companion object {
        private const val WAITING_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:WAITING:QUEUE"
        private const val ADMITTED_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:ADMITTED:QUEUE"
        private const val EVENT_DONE_MESSAGE = "Sorry but, event is over"
        private const val NOT_YET_JOIN_MESSAGE = "You didn't join yet"
    }

    private val waitingQueueKey = "${eventProperties.getEventName()}${WAITING_QUEUE_REDIS_KEY_POSTFIX}"
    private val admittedQueueKey = "${eventProperties.getEventName()}${ADMITTED_QUEUE_REDIS_KEY_POSTFIX}"

    private val admitLua = RedisScript.of(
        """
            local current = redis.call('llen', KEYS[2])
            local eventLimit = tonumber(redis.call('get', KEYS[3]))
            if current >= eventLimit then return 1 end
            local admit = math.min(eventLimit - current, tonumber(ARGV[1]))
            local admittedClients = redis.call('zrange', KEYS[1], 0, admit - 1)
            if #admittedClients == 0 then return 2 end
            redis.call('zremrangebyrank', KEYS[1], 0, admit - 1)
            redis.call('rpush', KEYS[2], unpack(admittedClients))
            return 3
            """, Int::class.java
    )
    private val admitLuaKeys = listOf(waitingQueueKey, admittedQueueKey, admittedQueueKey)

    private val joinLua = RedisScript.of(
        "redis.call('zadd', KEYS[1], ARGV[1], ARGV[2]);return redis.call('zrank', KEYS[1], ARGV[2]); ",
        Int::class.java
    )

    private var admittedClientCount = 0L
    private val logger = KotlinLogging.logger {}

    override fun join(client: String): JoinResult {
        if (admittedClientCount >= eventProperties.getEventLimit()) {
            return JoinResult.Fail(EVENT_DONE_MESSAGE)
        }
        admittedClientCount = lettuceClient.opsForSet().size(admittedQueueKey) ?: throw Exception()
        if (admittedClientCount >= eventProperties.getEventLimit()) {
            return JoinResult.Fail(EVENT_DONE_MESSAGE)
        }
        return System.nanoTime().let {
            JoinResult.Success(
                lettuceClient.execute(
                    joinLua,
                    listOf(waitingQueueKey),
                    listOf(it.toDouble(), client).toTypedArray()
                ), it
            )
        }
    }

    override fun admitNextClientsForStandalone(request: Int) {
        admitNextClientsForDistributed(request)
    }

    override fun admitNextClientsForDistributed(request: Int) =
        lettuceClient.execute(
            admitLua,
            admitLuaKeys,
            listOf(request).toTypedArray()
        ).let {
            when (it) {
                1 -> logger.info { "Event is over" }
                2 -> logger.info { "Waiting queue is empty" }
                3 -> logger.info { "Admit Success" }
                else -> throw Exception()
            }
        }


    override fun getWaitingRank(client: String): RankResult =
        lettuceClient.opsForZSet().rank(waitingQueueKey, client)
            ?.let { RankResult.Success(it.toInt()) }
            ?: RankResult.Fail(NOT_YET_JOIN_MESSAGE)

}