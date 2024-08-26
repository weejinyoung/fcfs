package taskforce.fcfs.allocate

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "fcfs.queue.admit")
data class YamlQueueAdmitProperties @ConstructorBinding constructor (
    val request: Long,
    val delay: Long
) : QueueAdmitProperties {
    override fun getAdmitRequest(): Long = request
    override fun getAdmitDelay(): Long = delay
}