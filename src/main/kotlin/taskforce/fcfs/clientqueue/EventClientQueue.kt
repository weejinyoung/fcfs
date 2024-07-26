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
     * Admit next clients
     * @return the client's identifier collections who admitted
     */
    fun admitNextClients(request: Int)

    fun getWaitingRank(client: String): RankResult
}