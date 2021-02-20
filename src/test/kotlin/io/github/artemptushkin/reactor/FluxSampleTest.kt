package io.github.artemptushkin.reactor

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.core.ThrowingRunnable
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.GroupedFlux
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import reactor.core.publisher.ConnectableFlux
import java.util.function.Consumer
import java.util.function.Predicate


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
        val publisher = Flux.just(1, 2, 3)

        StepVerifier
            .create(publisher)
            .expectNext(1)
            .expectNext(2)
            .expectNext(3)
            .verifyComplete()
        StepVerifier
            .create(publisher)
            .expectNext(1)
            .expectNext(2)
            .expectNext(3)
            .verifyComplete()
    }

    @Test
    fun itEmitsFromHotOnConnect() {
        val source: Flux<String> = Flux.fromIterable(listOf("ram", "sam", "dam", "lam"))
            .doOnNext { x -> println(x) }
            .filter { x -> x.startsWith("l") }
            .map { x -> x.toUpperCase() }

        val connectable = source.publish()

        StepVerifier
            .create(connectable)
            .expectSubscription()
            .expectNoEvent(ofMillis(200))
            .then(connectable::connect)
            .expectNext("LAM")
            .verifyComplete()
    }

    @Test
    fun itSubscribesToMultiplePublishersOnParallel() {
        val mainThreadName = Thread.currentThread().name
        val publisher1 = Flux.just(1, 2, 3).delayElements(ofMillis(200))
        val publisher2 = Flux.just(4, 5, 6).delayElements(ofMillis(100))
        var called = 0
        val consumer = Consumer<Int> {
            assertThat(Thread.currentThread().name).isNotEqualTo(mainThreadName)
            println(it)
            ++called
        }

        publisher1.subscribe(consumer)
        publisher2.subscribe(consumer)

        await.until { called == 6 }
    }
}