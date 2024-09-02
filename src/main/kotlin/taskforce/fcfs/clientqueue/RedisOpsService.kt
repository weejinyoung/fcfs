package taskforce.fcfs.clientqueue

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

@Service
class RedisOpsService (
    private val redissonClient: RedissonClient
){

    fun flushAll() {

    }

}