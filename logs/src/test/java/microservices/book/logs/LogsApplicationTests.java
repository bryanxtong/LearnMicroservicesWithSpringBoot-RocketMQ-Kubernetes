package microservices.book.logs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.apache.rocketmq.client.autoconfigure.RocketMQAutoConfiguration",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
class LogsApplicationTests {

	@Test
	void contextLoads() {
	}

}
