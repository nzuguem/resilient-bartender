package me.nzuguem.resilience.bartender;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Startup configuration for all external services on which the application depends.
 * They will be started before the application is launched.
 * It will be taken into account as a test resource in test classes -> @QuarkusTestResource(ExternalServicesResource.class)
 */
public class ExternalServicesResource implements QuarkusTestResourceLifecycleManager {

    private static final Network NETWORK = Network.newNetwork();

    private static final int BEERS_PROXY_CHAOS_MONKEY_PORT = 23679;

    public static final ToxiproxyContainer TOXI_PROXY = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withExposedPorts(8474, BEERS_PROXY_CHAOS_MONKEY_PORT)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("toxi-proxy")))
            .withNetwork(NETWORK);


    private static final int BEERS_SERVER_PORT =  1080;
    public static final MockServerContainer BEERS_SERVER = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"))
            .withNetworkAliases("beers-server")
            //.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("beers-server")))
            .withNetwork(NETWORK);

    static {

        // Start containers
        Startables.deepStart(TOXI_PROXY, BEERS_SERVER).join();
    }

    private static MockServerClient beersProxy;

    public Proxy beersToxiproxy;

    @Override
    public Map<String, String> start() {

        // Mock Return of BEERS_SERVER
        beersProxy = new MockServerClient(BEERS_SERVER.getHost(), BEERS_SERVER.getServerPort());
        beersProxy.when(
                HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/beers"))
                .respond(
                        HttpResponse.response()
                                .withStatusCode(200)
                                .withHeader(new Header("Content-Type", "application/json"))
                                .withBody(JsonBody.json("""
                                        [
                                          {
                                            "name" :  "Desperados"
                                          },
                                          {
                                            "name" :  "Saint Erwan"
                                          },
                                          {
                                            "name" :  "Corona"
                                          }
                                        ]
                                        """)
                                )
                );

        // Client for interacting with the TOXI_PROXY container (to create a proxy, for example)
        var toxiProxyClient = new ToxiproxyClient(TOXI_PROXY.getHost(), TOXI_PROXY.getControlPort());
        try {
            // Proxy creation, on which errors will be injected
            beersToxiproxy = toxiProxyClient.createProxy(
                    "beers-server", "0.0.0.0:%d".formatted(BEERS_PROXY_CHAOS_MONKEY_PORT),
                    "beers-server:%d".formatted(BEERS_SERVER_PORT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Overloading of configuration parameters
        return Map.of(
                "quarkus.rest-client.beers-proxy.url",
                "http://%s:%d".formatted(TOXI_PROXY.getHost(), TOXI_PROXY.getMappedPort(BEERS_PROXY_CHAOS_MONKEY_PORT)));
    }

    @Override
    public void stop() {

        // Stop container
        if (Objects.nonNull(TOXI_PROXY)) {
            TOXI_PROXY.stop();
        }

        if (Objects.nonNull(BEERS_SERVER)) {
            BEERS_SERVER.stop();
        }
    }

    // Create injectable proxies (NOT UNDER CDI CONTROL)
    @Override
    public void inject(TestInjector testInjector) {
       testInjector.injectIntoFields(this.beersToxiproxy, new TestInjector.AnnotatedAndMatchesType(InjectBeersToxiproxy.class, Proxy.class));
    }
}
