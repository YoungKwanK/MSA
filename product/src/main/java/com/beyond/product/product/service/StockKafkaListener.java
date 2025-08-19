package com.beyond.product.product.service;

import com.beyond.product.product.dto.ProductUpdateStockDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockKafkaListener {

    private final ProductService productService;

    @KafkaListener(topics = "stock-update-topic", containerFactory = "kafkaListener")
    public void stockConsumer(String message){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ProductUpdateStockDTO productUpdateStockDTO = objectMapper.readValue(message, ProductUpdateStockDTO.class);
            productService.updateStock(productUpdateStockDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
