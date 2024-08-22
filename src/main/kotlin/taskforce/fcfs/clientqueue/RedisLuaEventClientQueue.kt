package taskforce.fcfs.clientqueue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult


@Primary // TODO Redisson 종속 서비스 없애기
@Component
class RedisLuaEventClientQueue(
    private val eventProperties: EventProperties,
    private val lettuceClient: RedisTemplate<String, Any>
) : EventClientQueue<String> {

    companion object {
        private const val WAITING_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:WAITING:QUEUE"
        private const val ADMITTED_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:ADMITTED:QUEUE"
        private const val EVENT_DONE_MESSAGE = "Sorry but, event is over"
        private const val NOT_YET_JOIN_MESSAGE = "You didn't join yet"
    }

    private val waitingQueueKey = "${eventProperties.getEventName()}${WAITING_QUEUE_REDIS_KEY_POSTFIX}"
    private val admittedQueueKey = "${eventProperties.getEventName()}${ADMITTED_QUEUE_REDIS_KEY_POSTFIX}"

    /*
    KEYS[1] = admittedQueueKey
    KEYS[2] = waitingQueueKey
    ARGV[1] = eventLimit
    ARGV[2] = request
    */
    // TODO Queue 가 없을 시 생성하는 로직을 여기가 아닌 생성 시에 한 번 호출하는 것으로 바꿔주는 게 좋을까?
    private val luaOfAdmittingLogic = RedisScript.of(
        """
           if redis.call('exists', KEYS[1]) == 0 then 
               redis.call('sadd', KEYS[1], 'temp')  
               redis.call('srem', KEYS[1], 'temp')
           end
           local current = tonumber(redis.call('scard', KEYS[1]));
           if current >= tonumber(ARGV[1]) then 
               return 1
           end;
           local admit = math.min(tonumber(ARGV[1]) - current, tonumber(ARGV[2]));
           if redis.call('exists', KEYS[2]) == 0 then
               redis.call('zadd', KEYS[2], 0, 'temp')
               redis.call('zremrangebyrank', KEYS[2], 0, 0)  
           end
           local admittedClients = redis.call('zrange', KEYS[2], 0, admit - 1);
           if #admittedClients == 0 then
               return 2
           end;
           redis.call('zremrangebyrank', KEYS[2], 0, admit - 1);
           redis.call('sadd', KEYS[1], unpack(admittedClients));
           return 3
           """,
        Long::class.java
    )
    private val luaOfJoiningLogic = RedisScript.of(
        "redis.call('zadd', KEYS[1], ARGV[1], ARGV[2]);return redis.call('zrank', KEYS[1], ARGV[2]); ",
        Long::class.java
    )

    private val logger = KotlinLogging.logger {}

    override fun join(client: String): JoinResult {
        val current = lettuceClient.opsForSet().size(admittedQueueKey) ?: throw Exception()
        if (current >= eventProperties.getEventLimit()) {
            return JoinResult.Fail(EVENT_DONE_MESSAGE)
        }
        return System.nanoTime().let {
            JoinResult.Success(lettuceClient.execute(luaOfJoiningLogic, listOf(waitingQueueKey), it, client), it)
        }
    }

    override fun admitNextClientsForStandalone(request: Long) {
        admitNextClientsForDistributed(request)
    }

    override fun admitNextClientsForDistributed(request: Long) =
        lettuceClient.execute(
            luaOfAdmittingLogic,
            listOf(admittedQueueKey, waitingQueueKey),
            eventProperties.getEventLimit(),
            request
        ).let {
            when (it) {
                1L -> logger.info { "Event is over" }
                2L -> logger.info { "Waiting queue is empty" }
                3L -> logger.info { "Admit Success" }
                else -> throw Exception()
            }
        }

    override fun getWaitingRank(client: String): RankResult =
        lettuceClient.opsForZSet().rank(waitingQueueKey, client)
            ?.let { RankResult.Success(it.toInt()) }
            ?: RankResult.Fail(NOT_YET_JOIN_MESSAGE)
}