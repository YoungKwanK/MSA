package com.beyond.ordering.ordering.service;

import com.beyond.ordering.common.dto.CommonDTO;
import com.beyond.ordering.common.service.SseAlarmService;
import com.beyond.ordering.ordering.domain.OrderDetail;
import com.beyond.ordering.ordering.domain.Ordering;
import com.beyond.ordering.ordering.dto.OrderCreateDTO;
import com.beyond.ordering.ordering.dto.OrderListResDTO;
import com.beyond.ordering.ordering.dto.ProductDTO;
import com.beyond.ordering.ordering.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
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

    // 주문 생성
    public Long createFeignKafka(List<OrderCreateDTO> orderCreateDTOList, String email) {

        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();

        for (OrderCreateDTO orderCreateDTO : orderCreateDTOList) {
//            feign 클라이언트를 사용한 동기적 상품 조회
           
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

//            kafka를 활용한 비동기적 재고감소 요청

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
