package me.nzuguem.resilience.bartender;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@Path("beers")
//@RunOnVirtualThread
public class BeersResource {

    @ConfigProperty(name = "resilience.bartender.host")
    private String hostName;

    @ConfigProperty(name = "resilience.bartender.version")
    private String hostVersion;

    private final BeersService beersService;

    public BeersResource(BeersService beersService) {
        this.beersService = beersService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBeers() {
        var beers = this.beersService.getBeersK8s();

        var response = Map.of(
                "host", this.hostName,
                "version", this.hostVersion,
                "beers", beers
        );

        return Response.ok(response).build();
    }

}
