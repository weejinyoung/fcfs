package taskforce.fcfs.allocate

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import taskforce.fcfs.clientqueue.EventClientQueue

@Service
class FirstComeFirstServedEventService(
    private val eventClientQueue: EventClientQueue<String>,
    private val queueAdmitProperties: QueueAdmitProperties
) {

    fun joinClientQueue(client: String) =
        eventClientQueue.join(client)

    @Scheduled(fixedDelay = 500)
    private fun pollingClientQueue() =
        eventClientQueue.admitNextClientsForStandalone(queueAdmitProperties.getAdmitRequest())
}