package microservices.book.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.gateway.server.webflux.default-filters[0].name=Retry",
        "spring.cloud.gateway.server.webflux.default-filters[0].args.retries=3",
        "spring.cloud.gateway.server.webflux.default-filters[0].args.methods=GET"
})
class GatewayApplicationTests {

    @Autowired
    private org.springframework.cloud.gateway.route.RouteLocator routeLocator;

    @Test
    void contextLoads() {
        assertThat(routeLocator).isNotNull();
    }

}
