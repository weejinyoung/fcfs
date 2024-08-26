package taskforce.fcfs.clientqueue

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding


@ConfigurationProperties(prefix = "fcfs.event")
data class YamlEventProperties @ConstructorBinding constructor (
    val name: String,
    val limit: Long
) : EventProperties {
    override fun getEventName(): String = name
    override fun getEventLimit(): Long = limit
}