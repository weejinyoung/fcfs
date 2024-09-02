package taskforce.fcfs.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration
import java.util.concurrent.ScheduledFuture

@Configuration
class TaskSchedulerConfig {

    // TODO pool 설정
    @Bean
    fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            // TODO 3개일 필요가 있을까? 게다가 분산락은 스레드 id 로 키를 식별하는데 1개로 해야하지 않을까
            poolSize = 1
            setThreadNamePrefix("fcfs-scheduler-")
        }
}