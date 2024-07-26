package taskforce.fcfs.allocate

import org.springframework.stereotype.Component
import taskforce.fcfs.allocate.QueueAdmitProperties

@Component
class YamlQueueAdmitProperties : QueueAdmitProperties {
    override fun getRequest(): Int = 50
}
