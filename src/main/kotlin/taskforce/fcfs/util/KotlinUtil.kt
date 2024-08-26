package taskforce.fcfs.util

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration
import java.util.concurrent.ScheduledFuture


// for ThreadPoolTaskScheduler lambda expression
fun ThreadPoolTaskScheduler.scheduleWithFixedRate(
    duration: Duration,
    task: () -> Unit
): ScheduledFuture<*> {
    return this.scheduleAtFixedRate({ task() }, duration)
}