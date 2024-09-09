package taskforce.fcfs.clientqueue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult


// TODO Waiting Queue 의 최대 제한 설정... 이건 레디스에서 아니면 애플리케이션에서?
// TODO dis lock 으로 admit 하던 서비스 없애기
// TODO evalsha 로 스크립트 캐싱
@Primary
@Component
class RedisEventClientQueue(
    private val eventProperties: EventProperties,
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
    private val waitingQueue = redissonClient.getScoredSortedSet<String>(waitingQueueKey)
    private val scriptConnector = redissonClient.getScript(StringCodec.INSTANCE)
    private val logger = KotlinLogging.logger {}

    /*
    KEYS[1] = admittedQueueKey
    KEYS[2] = waitingQueueKey
    ARGV[1] = eventLimit
    ARGV[2] = request
    */
    // TODO admit 한 것을 가져와 데이터베이스에 바로 쓰는 전략 로직도 만들어놓자
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
            if redis.call('scard', KEYS[1]) >= tonumber(ARGV[1]) then
                return -1;
            else
                redis.call('zadd', KEYS[2], tonumber(ARGV[2]), ARGV[3]);
                return redis.call('zrank', KEYS[2], ARGV[3]);
            end
        """.trimIndent()

    override fun join(client: String): JoinResult {
        val joinTime = System.nanoTime()
        return scriptConnector.eval<Long>(
            RScript.Mode.READ_WRITE,
            luaOfJoiningLogic,
            RScript.ReturnType.VALUE,
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

    override fun admitClients(request: Long) =
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
        waitingQueue.rank(client)
            ?.let { RankResult.Success(it) }
            ?: RankResult.Fail(NOT_YET_JOIN_MESSAGE)

    override fun clear() {
        waitingQueue.clear()
        redissonClient.getSet<String>(admittedQueueKey).clear()
    }

    private fun cachingLuaScriptInRedis(script: String) =
        scriptConnector.scriptLoad(script)
}
