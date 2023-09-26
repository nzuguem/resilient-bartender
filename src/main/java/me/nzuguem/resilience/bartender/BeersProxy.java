package me.nzuguem.resilience.bartender;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("beers")
@RegisterRestClient(configKey = "beers-proxy")
public interface BeersProxy {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Beer> getBeers();

}
