package taskforce.fcfs.clientqueue


interface EventProperties {
    fun getEventName(): String
    fun getEventLimit(): Long
}