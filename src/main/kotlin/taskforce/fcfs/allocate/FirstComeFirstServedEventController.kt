package taskforce.fcfs.allocate

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FirstComeFirstServedEventController (
    private val firstComeFirstServedEventService: FirstComeFirstServedEventService
){

    @PostMapping("/event/client/queue")
    fun applyFirstComeFirstServedEvent(@RequestBody client: String) =
        ResponseEntity(firstComeFirstServedEventService.joinClientQueue(client), HttpStatus.OK)

    @DeleteMapping("/event/client/queue")
    fun resetFirstComeFirstServedEvent() {
        ResponseEntity(firstComeFirstServedEventService.clearClientQueue(), HttpStatus.OK)
    }
}