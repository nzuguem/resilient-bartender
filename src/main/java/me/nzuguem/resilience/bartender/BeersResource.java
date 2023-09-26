package me.nzuguem.resilience.bartender;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Path("beers")
public class BeersResource {

    private final BeersService beersService;

    public BeersResource(BeersService beersService) {
        this.beersService = beersService;
    }


    @GET
    @Path("timeout")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public List<Beer> getBeersTimeout() {
        return this.beersService.getBeersTimeout();
    }

    @GET
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public List<Beer> getBeersRetry() {
        return this.beersService.getBeersRetry(new AtomicInteger(1));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public List<Beer> getBeers() {
        return this.beersService.getBeers();
    }

}
