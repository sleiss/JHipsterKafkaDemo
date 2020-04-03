package com.mycompany.myapp.config.websocket;

import com.mycompany.myapp.config.KafkaProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.security.Principal;

@Component
public class MessageLoggerInterceptor implements ChannelInterceptor {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(MessageLoggerInterceptor.class);

    private KafkaTemplate<String, String> kafkaTemplate;

    public MessageLoggerInterceptor(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String msg) {
        ListenableFuture<SendResult<String, String>> future =
            kafkaTemplate.send("websocket", msg);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                logger.info("Sent message=[" + msg +
                    "] with offset=[" + result.getRecordMetadata().offset() + "]");
            }
            @Override
            public void onFailure(Throwable ex) {
                logger.info("Unable to send message=["
                    + msg + "] due to : " + ex.getMessage());
            }
        });
    }

    @KafkaListener(topics = "websocket", groupId = "${spring.application.name}")
    public void listen(String message) {
        logger.info("Received message in group ${spring.application.name}: " + message);
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        logger.info("Received message {} from channel {}", message.toString(), channel.toString());
        sendMessage(message.toString());
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        sendMessage(message.toString());
    }
}
