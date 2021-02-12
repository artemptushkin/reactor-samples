package io.github.artemptushkin.reactor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration

class MonoSampleTest {

    @Test
    fun itEmitsElementOnDifferentThread() {
        val mono = Mono.just { "foo" }.subscribeOn(Schedulers.parallel())

        val thisThreadName = Thread.currentThread().name

        StepVerifier
            .create(mono.map { Thread.currentThread().name })
            .assertNext { t -> assertThat(t).isNotEqualTo(thisThreadName) }
            .expectComplete()
            .verify()
    }

    @Test
    fun itEmitsElementsWithDelay() {
        val mono = Mono.just("foo").delayElement(Duration.ofSeconds(1))

        val thisThreadName = Thread.currentThread().name

        StepVerifier
            .withVirtualTime { mono }
            .thenAwait(Duration.ofSeconds(1))
            .assertNext { t -> assertThat(t).isNotEqualTo(thisThreadName) }
            .expectComplete()
            .verify()
    }
}