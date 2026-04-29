package microservices.book.gamification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.apache.rocketmq.client.autoconfigure.RocketMQAutoConfiguration",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
@ActiveProfiles("test")
class GamificationApplicationTests {

    @Test
    void contextLoads() {
    }

}
