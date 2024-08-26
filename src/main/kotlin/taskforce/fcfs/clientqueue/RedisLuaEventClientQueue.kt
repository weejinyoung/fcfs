package taskforce.fcfs.clientqueue

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult
import kotlin.math.log


@Primary // TODO dis lock 으로 admit 하던 서비스 없애기
@Component
class RedisLuaEventClientQueue(
    private val eventProperties: EventProperties,
    private val lettuceClient: RedisTemplate<String, Any>,
    private val redissonClient: RedissonClient
) : EventClientQueue<String> {

    companion object {
        private const val WAITING_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:WAITING:QUEUE"
        private const val ADMITTED_QUEUE_REDIS_KEY_POSTFIX = ":CLIENT:ADMITTED:QUEUE"
        private const val EVENT_DONE_MESSAGE = "Sorry but, event is over"
        private const val NOT_YET_JOIN_MESSAGE = "You didn't join yet"
    }

    private val waitingQueueKey = "${eventProperties.getEventName()}${WAITING_QUEUE_REDIS_KEY_POSTFIX}"
    private val admittedQueueKey = "${eventProperties.getEventName()}${ADMITTED_QUEUE_REDIS_KEY_POSTFIX}"
    private val scriptConnector = redissonClient.getScript(StringCodec.INSTANCE)
    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun checkDependenciesInjections() {
        logger.info { "eventProperties ${eventProperties.getEventLimit()}" }
    }

    /*
    KEYS[1] = admittedQueueKey
    KEYS[2] = waitingQueueKey
    */
//    private val luaOfInitializingClientQueueLogic =
//        """
//           if redis.call('exists', KEYS[1]) == 0 then
//               redis.call('sadd', KEYS[1], 'temp')
//               redis.call('srem', KEYS[1], 'temp')
//           end
//           if redis.call('exists', KEYS[2]) == 0 then
//               redis.call('zadd', KEYS[2], 0, 'temp')
//               redis.call('zremrangebyrank', KEYS[2], 0, 0)
//           end
//        """.trimIndent()

    /*
    KEYS[1] = admittedQueueKey
    KEYS[2] = waitingQueueKey
    ARGV[1] = eventLimit
    ARGV[2] = request
    */
    private val luaOfAdmittingLogic =
        """
           local current = redis.call('scard', KEYS[1]);
           if current >= tonumber(ARGV[1]) then 
               return 1
           end;
           local admit = math.min(tonumber(ARGV[1]) - current, tonumber(ARGV[2]));
           local admittedClients = redis.call('zrange', KEYS[2], 0, admit - 1);
           if #admittedClients == 0 then
               return 2
           end;
           redis.call('zremrangebyrank', KEYS[2], 0, admit - 1);
           redis.call('sadd', KEYS[1], unpack(admittedClients));
           return 3
        """.trimIndent()

    /*
    KEYS[1] = admittedQueueKey
    KEYS[2] = waitingQueueKey
    ARGV[1] = eventLimit
    ARGV[2] = joinTime
    ARGV[3] = client
    */
    private val luaOfJoiningLogic =
        """
            if redis.call('zcard', KEYS[1]) >= tonumber(ARGV[1]) then
                return -1
            else
                redis.call('zadd', KEYS[2], tonumber(ARGV[2]), ARGV[3])
                return rank = redis.call('zrank', KEYS[2], ARGV[3])
            end
        """.trimIndent()

//    @PostConstruct
//    private fun initClientQueue() {
//        scriptConnector.eval<Any>(
//            RScript.Mode.READ_WRITE,
//            luaOfInitializingClientQueueLogic,
//            RScript.ReturnType.VALUE,
//            listOf(admittedQueueKey, waitingQueueKey)
//        )
//    }

    override fun join(client: String): JoinResult {
        val joinTime = System.nanoTime()
        return scriptConnector.eval<Long>(
            RScript.Mode.READ_WRITE,
            luaOfJoiningLogic,
            RScript.ReturnType.INTEGER,
            listOf(admittedQueueKey, waitingQueueKey),
            eventProperties.getEventLimit(),
            joinTime,
            client
        ).let {
            when (it) {
                -1L -> JoinResult.Fail(EVENT_DONE_MESSAGE)
                else -> JoinResult.Success(it, joinTime)
            }
        }
    }

    override fun admitNextClients(request: Long) =
        scriptConnector.eval<Long>(
            RScript.Mode.READ_WRITE,
            luaOfAdmittingLogic,
            RScript.ReturnType.VALUE,
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

//    override fun join(client: String): JoinResult {
//        val current = lettuceClient.opsForSet().size(admittedQueueKey) ?: throw Exception()
//        if (current >= eventProperties.getEventLimit()) {
//            return JoinResult.Fail(EVENT_DONE_MESSAGE)
//        }
//        return System.nanoTime().let {
//            JoinResult.Success(lettuceClient.execute(luaOfJoiningLogic, listOf(waitingQueueKey), it, client), it)
//        }
//    }

//    override fun admitNextClients(request: Long) =
//        lettuceClient.execute(
//            luaOfAdmittingLogic,
//            listOf(admittedQueueKey, waitingQueueKey),
//            eventProperties.getEventLimit(),
//            request
//        ).let {
//            when (it) {
//                1L -> logger.info { "Event is over" }
//                2L -> logger.info { "Waiting queue is empty" }
//                3L -> logger.info { "Admit Success" }
//                else -> throw Exception()
//            }
//        }
