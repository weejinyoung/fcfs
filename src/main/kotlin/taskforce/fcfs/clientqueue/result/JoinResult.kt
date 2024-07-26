package taskforce.fcfs.clientqueue.result

import java.time.LocalDateTime

sealed class JoinResult {
    data class Success(val rank: Int, val joinTime: LocalDateTime): JoinResult()
    data class Fail(val message: String): JoinResult()
}