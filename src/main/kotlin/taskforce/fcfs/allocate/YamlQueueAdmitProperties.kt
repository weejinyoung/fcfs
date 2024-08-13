package taskforce.fcfs.allocate

import org.springframework.stereotype.Component
import taskforce.fcfs.allocate.QueueAdmitProperties

@Component
class YamlQueueAdmitProperties : QueueAdmitProperties {
    override fun getRequest(): Long = 50L
}
