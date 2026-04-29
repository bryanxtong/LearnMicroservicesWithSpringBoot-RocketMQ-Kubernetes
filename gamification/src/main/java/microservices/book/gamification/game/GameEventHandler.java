package microservices.book.gamification.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.book.event.challenge.ChallengeSolvedEvent;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
@Service
@RocketMQMessageListener(consumerGroup = "gamification", topic = "${rocketmq.attempts}", requestTimeout = 10000)
public class GameEventHandler implements RocketMQListener {
    private final GameService gameService;
    private final ObjectMapper objectMapper;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        try {
            String event = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
            ChallengeSolvedEvent challengeSolvedEvent = objectMapper.readValue(event, ChallengeSolvedEvent.class);
            log.info("Challenge Solved Event received: {}", challengeSolvedEvent.getAttemptId());
            gameService.newAttemptForUser(challengeSolvedEvent);
        } catch (final Exception e) {
            log.error("Error when trying to process ChallengeSolvedEvent", e);
            return ConsumeResult.FAILURE;
        }
        return ConsumeResult.SUCCESS;
    }
}
