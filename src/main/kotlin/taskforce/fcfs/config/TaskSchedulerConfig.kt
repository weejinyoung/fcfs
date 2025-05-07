package taskforce.fcfs.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration
import java.util.concurrent.ScheduledFuture

@Configuration
class TaskSchedulerConfig {

    @Bean
    fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            // 현재 선착순 락을 위한 용도 뿐이므로 스레드는 하나
            poolSize = 1
            setThreadNamePrefix("fcfs-scheduler-")
        }
}