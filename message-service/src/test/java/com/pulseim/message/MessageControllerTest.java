package com.pulseim.message;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.security.JwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pulseim_message;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.host=127.0.0.1",
        "spring.rabbitmq.port=1"
})
class MessageControllerTest {
    @Autowired
    private MessageController controller;
    @Autowired
    private MessageRepository messages;

    @Test
    void repeatsClientMessageIdWithoutCreatingAnotherMessage() {
        String authorization = "Bearer " + JwtSupport.issue("u-sender", "web", Duration.ofMinutes(1));
        MessageController.SendMessageCommand command = new MessageController.SendMessageCommand("client-1", "c-1", "u-recipient", "hello", null, null, null);

        ApiResponse<MessageView> first = controller.send(authorization, "trace-a", command);
        ApiResponse<MessageView> repeated = controller.send(authorization, "trace-b", command);

        assertThat(repeated.data().id()).isEqualTo(first.data().id());
        assertThat(repeated.data().sequence()).isEqualTo(1L);
        assertThat(messages.count()).isEqualTo(1L);
    }
}
