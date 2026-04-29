package microservices.book.multiplication;

import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.apache.rocketmq.client.autoconfigure.RocketMQAutoConfiguration",
		"spring.cloud.nacos.config.enabled=false",
		"spring.cloud.nacos.discovery.enabled=false"
})
@ActiveProfiles("test")
class MultiplicationApplicationTests {

	@MockitoBean
	private RocketMQClientTemplate rocketMQClientTemplate;

	@Test
	void contextLoads() {
	}

}
