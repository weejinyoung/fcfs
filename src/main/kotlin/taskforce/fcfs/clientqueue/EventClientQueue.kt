package taskforce.fcfs.clientqueue

import taskforce.fcfs.clientqueue.result.JoinResult
import taskforce.fcfs.clientqueue.result.RankResult

interface EventClientQueue<T> {

    /**
     * Client join event queue.
     * @return client's rank from the queue.
     */
    fun join(client: T): JoinResult
    /**
     * Admit next clients for standalone environment
     */
    fun admitNextClientsForStandalone(request: Int)
    /**
     * Admit next clients for distributed environment
     */
    fun admitNextClientsForDistributed(request: Int)

    fun getWaitingRank(client: String): RankResult
}