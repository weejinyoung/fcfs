package taskforce.fcfs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import taskforce.fcfs.allocate.YamlQueueAdmitProperties
import taskforce.fcfs.clientqueue.YamlEventProperties

@EnableScheduling
@EnableConfigurationProperties(YamlEventProperties::class, YamlQueueAdmitProperties::class)
@SpringBootApplication
class FcfsApplication

fun main(args: Array<String>) {
	runApplication<FcfsApplication>(*args)
}
