package taskforce.fcfs

import com.github.f4b6a3.tsid.TsidCreator
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentLinkedDeque

// 약 7000 개의 쿠폰을 발급해야한다면?! 메모리 부족 문제, 결국 디비에 적재를 해야하는 것인가
// 지금 스레드풀의 느낌이다
// 저장해주지 말고 필요할 때 만들어서 주고 gc 의 도움을 받아야하나..
object CodeFactory {

    private val logger = KotlinLogging.logger {}

    private val codeRepository = ConcurrentLinkedDeque<String>()

    fun init(limit: Int) =
        codeRepository.run {
            clear()
            repeat(limit) { offer(TsidCreator.getTsid().toString()) }
        }

    fun getCode(): String =
        codeRepository.run {
            ifEmpty { NoMoreCodeException() }
            poll()
        }

    fun getCodes() {
        logger.info { codeRepository }
    }
}