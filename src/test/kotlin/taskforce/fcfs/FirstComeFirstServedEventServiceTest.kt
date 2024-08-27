package taskforce.fcfs

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.StringSpec
import org.springframework.boot.test.context.SpringBootTest
import taskforce.fcfs.allocate.FirstComeFirstServedEventService
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


// TODO 제대로 된 단위테스트를 공부해보자
@SpringBootTest
class FirstComeFirstServedEventServiceTest (
//    private val eventService: FirstComeFirstServedEventService
): StringSpec({
//
//    "선착순 테스트" {
//        val logger = KotlinLogging.logger{}
//        fun addQueueWorker(countDownLatch: CountDownLatch) {
//            eventService.joinClientQueue(Thread.currentThread().toString()).also {
//                logger.info { "${Thread.currentThread().name} 가 진입합니다 순위는 $it 시간은 ${System.currentTimeMillis()}" }
//            }
//            countDownLatch.countDown()
//        }
//
//        val people = 100
//        val countDownLatch = CountDownLatch(1000)
//
//        val workers = (1..people)
//            .map { thread(start = false) { addQueueWorker(countDownLatch)} }
//
//        workers.forEach(Thread::start)
//        countDownLatch.await()
//        Thread.sleep(5000)
//    }

    "스케줄러 테스트" {

    }
})