package taskforce.fcfs.allocate

import taskforce.fcfs.clientqueue.EventClientQueue
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class FirstComeFirstServedEventService(
    private val eventClientQueue: EventClientQueue<String>,
    private val queueAdmitProperties: QueueAdmitProperties
) {

    fun joinClientQueue(client: String) =
        eventClientQueue.join(client)

    @Scheduled(fixedDelay = 500)
    private fun admitClientsInQueueScheduler() =
        eventClientQueue.admitNextClients(queueAdmitProperties.getRequest())
}