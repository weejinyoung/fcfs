package taskforce.fcfs.clientqueue.result


sealed class JoinResult {
    data class Success(val rank: Int, val joinTime: Double): JoinResult()
    data class Fail(val message: String): JoinResult()
}