package io.github.steliospaps.reactive.cache.demo;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import reactor.core.Disposable;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class DemoApplication {
	private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);
	public static void main(String[] args) throws InterruptedException {
		//SpringApplication.run(DemoApplication.class, args);

		EmitterProcessor<Object> completer = EmitterProcessor.create();
		Mono<Object> completerMono = completer.ignoreElements().cache();


		Flux<Long> a = Flux.interval(Duration.ofSeconds(1))
			.takeUntilOther(completerMono)
			.onBackpressureDrop(i->log.info("a dropping {}",i))
			.flatMap(i->Mono.just(i))
			.log("a")
			.cache(1)
			;
    Flux<Long> b = Flux.interval(Duration.ofSeconds(1)).log("b")
			.onBackpressureDrop(i->log.info("b dropping {}",i))
			.flatMap(i->Mono.just(i))
			.cache(1);

		Flux<String> c = Flux.combineLatest(a, b, (a1,b1)->a1+"."+b1)
			.log("b+a");
		
		//c.subscribe(); //does not resolve issue

		Flux<String> d = Flux.interval(Duration.ofSeconds(1))
			.filter(i->i==0)
			.switchMap(i -> c)
			.log("d");

		Flux.interval(Duration.ofMinutes(1))//
			.takeUntilOther(completerMono)
			.filter(x -> false)//
			.log("c")
			.withLatestFrom(d, (i,j)->i+"."+j)
			.log("c+b+a")
			.flatMap(i->Mono.just(i))
			.limitRate(6)
			.takeUntilOther(completerMono)
			.subscribe();
		

		TimeUnit.MINUTES.sleep(1);

		completer.onComplete();
		//cba.dispose();
	}

}
