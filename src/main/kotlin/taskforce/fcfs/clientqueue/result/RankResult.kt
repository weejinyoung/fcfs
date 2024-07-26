package taskforce.fcfs.clientqueue.result

sealed class RankResult {
    data class Success(val rank: Int): RankResult()
    data class Fail(val message: String): RankResult()
}