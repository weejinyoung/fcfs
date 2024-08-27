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
            poolSize = 3
            setThreadNamePrefix("fcfs-scheduler-")
            initialize()
        }
}