package com.beyond.ordering.ordering.service;

import com.beyond.ordering.common.config.KafkaProducerConfig;
import com.beyond.ordering.common.dto.CommonDTO;
import com.beyond.ordering.common.service.SseAlarmService;
import com.beyond.ordering.ordering.domain.OrderDetail;
import com.beyond.ordering.ordering.domain.Ordering;
import com.beyond.ordering.ordering.dto.OrderCreateDTO;
import com.beyond.ordering.ordering.dto.OrderListResDTO;
import com.beyond.ordering.ordering.dto.ProductDTO;
import com.beyond.ordering.ordering.feignclient.ProductFeignClient;
import com.beyond.ordering.ordering.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SseAlarmService sseAlarmService;
    private final RestTemplate restTemplate;
    private final ProductFeignClient productFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 주문 생성
    public Long save(List<OrderCreateDTO> orderCreateDTOList, String email) {

        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();

        for (OrderCreateDTO orderCreateDTO : orderCreateDTOList) {
//            상품 조회
            String productDetailUrl = "http://product-service/product/detail/" + orderCreateDTO.getProductId();
            HttpHeaders headers = new HttpHeaders();
//            HttpEntity : httpbody와 httpheader를 세팅하기 위한 객체
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<CommonDTO> response = restTemplate.exchange(productDetailUrl, HttpMethod.GET, httpEntity, CommonDTO.class);
            CommonDTO commonDTO = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
//            readValue : String -> Class 변환, convertValue : Object -> Class 변환
            ProductDTO product = objectMapper.convertValue(commonDTO.getResult(), ProductDTO.class);

//            재고 조회
            if (product.getStockQuantity() < orderCreateDTO.getProductCount()) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }

//            주문 발생
            OrderDetail orderDetail = OrderDetail.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(orderCreateDTO.getProductCount())
                    .ordering(ordering)
                    .build();

            ordering.getOrderDetailList().add(orderDetail);

//            동기적 재고감소 요청
            String productUpdateStockUrl = "http://product-service/product/updatestock";
            HttpHeaders stockHeaders = new HttpHeaders();
            stockHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrderCreateDTO> updateStockEntity = new HttpEntity<>(orderCreateDTO,stockHeaders);
            restTemplate.exchange(productUpdateStockUrl, HttpMethod.PUT, updateStockEntity, Void.class);
        }

//         주문 성공 시 admin 유저에게 알림 메세지 전송
        sseAlarmService.publishMessage("admin@email.com", email, ordering.getId());

        // db 저장
        orderRepository.save(ordering);

        return ordering.getId();
    }

//    fallback메서드는 원본 메서드의 매개변수와 정확히 일치해야 함.
    public void fallbackProductServiceCircuit(List<OrderCreateDTO> orderCreateDTOList, String email, Throwable t){
        throw new RuntimeException("상품서버 응답없음. 나중에 다시 시도해주세요.");
    }
    
//    테스트 : 4 ~ 5번의 정상 요청 -> 5번 중에 2번의 지연발생 -> circuit open -> 그 다음 요청은 바로 fallback

    // 주문 생성
    @CircuitBreaker(name = "productServiceCircuit", fallbackMethod = "fallbackProductServiceCircuit")
    public Long createFeignKafka(List<OrderCreateDTO> orderCreateDTOList, String email) {

        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();

        for (OrderCreateDTO orderCreateDTO : orderCreateDTOList) {
//            feign 클라이언트를 사용한 동기적 상품 조회
            CommonDTO commonDTO = productFeignClient.getProductById(orderCreateDTO.getProductId());
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDTO product = objectMapper.convertValue(commonDTO.getResult(), ProductDTO.class);
           
//            재고 조회
            if (product.getStockQuantity() < orderCreateDTO.getProductCount()) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }

//            주문 발생
            OrderDetail orderDetail = OrderDetail.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(orderCreateDTO.getProductCount())
                    .ordering(ordering)
                    .build();

            ordering.getOrderDetailList().add(orderDetail);
            
//            feign을 통한 동기적 재고감소 요청
//            productFeignClient.updateProductStockQuantity(orderCreateDTO);

//            kafka를 활용한 비동기적 재고감소 요청
            kafkaTemplate.send("stock-update-topic", orderCreateDTO);
        }

//         주문 성공 시 admin 유저에게 알림 메세지 전송
        sseAlarmService.publishMessage("admin@email.com", email, ordering.getId());
        // db 저장
        orderRepository.save(ordering);

        return ordering.getId();
    }

    // 주문 목록 조회
    public List<OrderListResDTO> findAll() {
        return orderRepository.findAll().stream()
                .map(OrderListResDTO::fromEntity).collect(Collectors.toList());
    }

    // 나의 주문 목록 조회
    public List<OrderListResDTO> myOrders(String email) {
        return  orderRepository.findAllByMemberEmail(email).stream()
                .map(OrderListResDTO::fromEntity).collect(Collectors.toList());
    }
}
