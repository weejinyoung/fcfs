package taskforce.fcfs.allocate

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "spring.fcfs.queue")
data class YamlQueueAdmitProperties @ConstructorBinding constructor (
    val request: Long
) : QueueAdmitProperties {
    override fun getAdmitRequest(): Long = request
}