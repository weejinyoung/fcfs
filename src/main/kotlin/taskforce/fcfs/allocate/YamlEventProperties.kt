package taskforce.fcfs.allocate

import taskforce.fcfs.clientqueue.EventProperties
import org.springframework.stereotype.Component

// TODO config 와 return bean 으로 관리해서 유연성 확보 필요
@Component
class YamlEventProperties : EventProperties {

    override fun getEventName(): String  = "CHICKEN"
    override fun getEventLimit(): Int = 1000
}