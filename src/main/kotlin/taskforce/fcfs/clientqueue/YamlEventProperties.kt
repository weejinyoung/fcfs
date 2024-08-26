package taskforce.fcfs.clientqueue

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

// TODO config 와 return bean 으로 관리해서 유연성 확보 필요

@ConfigurationProperties(prefix = "fcfs.event")
data class YamlEventProperties @ConstructorBinding constructor (
    val name: String,
    val limit: Long
) : EventProperties {
    override fun getEventName(): String = name
    override fun getEventLimit(): Long = limit
}