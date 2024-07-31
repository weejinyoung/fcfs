package taskforce.fcfs.clientqueue

// TODO 이벤트 종류에 변동이 있을 시, mapper 패턴 도입 고려
// TODO 쌩 빈 주입보단 @ConfigurationProperties 와 @EnableConfigurationProperties 을 쓰는 게 나을듯
interface EventProperties {
    fun getEventName(): String
    fun getEventLimit(): Int
}