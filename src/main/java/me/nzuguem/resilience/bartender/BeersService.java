package me.nzuguem.resilience.bartender;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class BeersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeersService.class);

    private final BeersProxy beersProxy;


    public BeersService(@RestClient  BeersProxy beersProxy) {
        this.beersProxy = beersProxy;
    }

    @Timeout(600)
    @Fallback(fallbackMethod = "getDefaultBeers")
    public List<Beer> getBeersTimeout() {
        return this.beersProxy.getBeers();
    }

    @Fallback(fallbackMethod = "getDefaultBeers")
    @Retry(delay = 2, delayUnit = ChronoUnit.SECONDS, maxRetries = 2)
    public List<Beer> getBeersRetry(AtomicInteger atomicInteger) {

        LOGGER.info("Attempt - {}", atomicInteger.getAndIncrement());

        return this.beersProxy.getBeers();
    }

    public List<Beer> getDefaultBeers() {
        return List.of(Beer.of("fallback"));
    }

    // the fallback signature must match that of the original method
    public List<Beer> getDefaultBeers(AtomicInteger atomicInteger) {
        return List.of(Beer.of("fallback"));
    }

    public List<Beer> getBeers() {
        return this.beersProxy.getBeers();
    }
}
