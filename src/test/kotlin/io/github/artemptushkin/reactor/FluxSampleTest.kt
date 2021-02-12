package io.github.artemptushkin.reactor

import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.GroupedFlux
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds


class FluxSampleTest {

    @Test
    fun itConcats() {
        StepVerifier.create(
            Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
                .groupBy { i: Int -> if (i % 2 == 0) "even" else "odd" }
                .concatMap { g: GroupedFlux<String, Int> ->
                    g.defaultIfEmpty(-1) //if empty groups, show them
                        .map { obj: Int -> obj.toString() } //map to string
                        .startWith(g.key())
                } //start with the group's key
        )
            .expectNext("odd", "1", "3", "5", "11", "13")
            .expectNext("even", "2", "4", "6", "12")
            .verifyComplete()
    }

    @Test
    fun itThrottles() {
        val publisher = Flux.just(1, 2, 3).delayElements(ofMillis(500))

        StepVerifier
            .withVirtualTime { publisher }
            .expectNext(1)
            .thenAwait(ofMillis(500))
            .expectNext(2)
            .thenAwait(ofMillis(500))
            .expectNext(3)
            .thenAwait(ofMillis(500))
            .verifyComplete()
    }

    @Test
    fun itEmitsToMultipleSubscribers() {
        val duration1 = (0..500L).random()
        val duration2 = (0..500L).random()
        val duration3 = (0..500L).random()
        val publisher = Flux
            .just(1)
            .delayElements(ofMillis(duration1))
            .concatWith { Flux.just(2).delayElements(ofMillis(duration2)) }
            .concatWith { Flux.just(3).delayElements(ofMillis(duration3)) }

        publisher.subscribe { println(it) }
        publisher.subscribe { println(it) }
        Thread.sleep(2000)
    }
}