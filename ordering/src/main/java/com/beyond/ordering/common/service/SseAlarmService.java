package com.beyond.ordering.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.beyond.ordering.common.dto.SseMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
@Slf4j
public class SseAlarmService implements MessageListener {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final RedisTemplate<String, String> redisTemplate;

    public SseAlarmService(SseEmitterRegistry sseEmitterRegistry
            , @Qualifier("ssePubSub") RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
    }

    public void publishMessage(String receiver, String sender, Long orderingId) {
        SseMessageDTO sseMessageDTO = SseMessageDTO.builder()
                .sender(sender)
                .receiver(receiver)
                .orderingId(orderingId).build();

        ObjectMapper objectMapper = new ObjectMapper();
        String data;
        try {
            data = objectMapper.writeValueAsString(sseMessageDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(receiver);

        if (sseEmitter != null) {
            try {
                sseEmitter.send(SseEmitter.event().name("ordered").data(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            redisTemplate.convertAndSend("order-channel",  data);
        }
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SseMessageDTO sseMessageDTO = objectMapper.readValue(message.getBody(), SseMessageDTO.class);
            String channelName = new String(pattern);
            log.info("sseMessageDTO = {}", sseMessageDTO);
            log.info("pattern = {}", channelName);

            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(sseMessageDTO.getReceiver());

            if (sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name("ordered").data(sseMessageDTO));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
