package me.nzuguem.resilience.bartender;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ExternalServicesResource.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BeersResourceTest {

    @InjectBeersToxiproxy
    private Proxy beersToxiproxy;

    @BeforeEach
    void setup() {

        // Assume
        Assumptions.assumeTrue(ExternalServicesResource.BEERS_SERVER.isRunning());
        Assumptions.assumeTrue(ExternalServicesResource.TOXI_PROXY.isRunning());
    }


    @Test
    void Should_getBeers() throws IOException {

        // Act / Assert
        given()
                .when()
                .get("/beers")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", Is.is(3));
    }

    @Test
    void Should_getFallbackBeers_When_Timeout() throws IOException {

        // Arrange / Inject network error
        var timeoutToxic = this.beersToxiproxy
                .toxics()
                .timeout("beers-server-timeout", ToxicDirection.DOWNSTREAM, 1000);

        // Act
        var beers = given()
                        .when()
                        .get("/beers/timeout")
                        .then()
                        .log().all()
                        .assertThat()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("size()", Is.is(1))
                        .extract()
                        .as(Beer[].class);

        // Assert
        assertThat(beers).containsExactly(Beer.of("fallback"));

        // clean toxics
        timeoutToxic.remove();
    }

    @Test
    void Should_getFallbackBeers_When_NetworkError_And_Retry_Reached() throws IOException {

        // Arrange / Inject network  error
        var resetPeerToxic = this.beersToxiproxy
                .toxics()
                .resetPeer("beers-server-resetPeer", ToxicDirection.DOWNSTREAM, 5);

        // Act
        var beers = given()
                        .when()
                        .get("/beers/retry")
                        .then()
                        .log().all()
                        .assertThat()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("size()", Is.is(1))
                        .extract()
                        .as(Beer[].class);

        // Assert
        assertThat(beers).containsExactly(Beer.of("fallback"));

        // clean toxics
       resetPeerToxic.remove();
    }

}
