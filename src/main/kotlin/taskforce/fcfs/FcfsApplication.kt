package taskforce.fcfs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class FcfsApplication

fun main(args: Array<String>) {
	runApplication<FcfsApplication>(*args)
}
