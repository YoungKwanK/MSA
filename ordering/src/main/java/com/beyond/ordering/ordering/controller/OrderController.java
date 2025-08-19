package com.beyond.ordering.ordering.controller;

import com.beyond.ordering.common.dto.CommonDTO;
import com.beyond.ordering.ordering.domain.Ordering;
import com.beyond.ordering.ordering.dto.OrderCreateDTO;
import com.beyond.ordering.ordering.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordering")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody List<OrderCreateDTO> orderCreateDTOList, @RequestHeader("X-User-Email") String email) {
        Long id = orderService.createFeignKafka(orderCreateDTOList, email);
        return new ResponseEntity<>(CommonDTO.builder()
                .result(id)
                .status_code(HttpStatus.CREATED.value())
                .status_message("주문 완료").build(), HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public ResponseEntity<?> findAll() {
        return new ResponseEntity<>(CommonDTO.builder()
                .result(orderService.findAll())
                .status_code(HttpStatus.OK.value())
                .status_message("주문 목록 조회 성공").build(), HttpStatus.OK);
    }

    @GetMapping("/myorders")
    public ResponseEntity<?> myOrders(@RequestHeader("X-User-Email") String email) {
        return new ResponseEntity<>(CommonDTO.builder()
                .result(orderService.myOrders(email))
                .status_code(HttpStatus.OK.value())
                .status_message("나의 주문 목록 조회 성공").build(), HttpStatus.OK);
    }
}