package taskforce.fcfs

import taskforce.fcfs.allocate.FirstComeFirstServedEventService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.StringSpec
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


@SpringBootTest
class CouponEventTest (
    private val eventService: FirstComeFirstServedEventService
): StringSpec({

    "쿠폰 발급 테스트" {
        val logger = KotlinLogging.logger{}
        fun addQueueWorker(countDownLatch: CountDownLatch) {
            eventService.joinClientQueue(Thread.currentThread().toString()).also {
                logger.info { "${Thread.currentThread().name} 가 진입합니다 순위는 $it 시간은 ${System.currentTimeMillis()}" }
            }
            countDownLatch.countDown()
        }

        val people = 100
        val countDownLatch = CountDownLatch(1000)

        val workers = (1..people)
            .map { thread(start = false) { addQueueWorker(countDownLatch)} }

        workers.forEach(Thread::start)
        countDownLatch.await()
        Thread.sleep(5000)
    }

    "코드 생성" {
        CodeFactory.init(1000)
        CodeFactory.getCodes()
    }
})