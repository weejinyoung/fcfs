package taskforce.fcfs.allocate

interface QueueAdmitProperties {
    fun getAdmitRequest(): Long
    fun getAdmitDelay(): Long
}