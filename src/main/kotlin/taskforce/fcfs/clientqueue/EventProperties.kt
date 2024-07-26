package taskforce.fcfs.clientqueue

// TODO 이벤트 종류에 변동이 있을 시, mapper 패턴 도입 고려
interface EventProperties {
    fun getEventName(): String
    fun getEventLimit(): Int
}