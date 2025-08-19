package com.beyond.ordering.ordering.feignclient;

import com.beyond.ordering.common.dto.CommonDTO;
import com.beyond.ordering.ordering.dto.OrderCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// name부분은 eureka에 등록된 application.name을 의미
@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/product/detail/{productId}")
    CommonDTO getProductById(@PathVariable("productId") Long productId);

    @PutMapping("/product/updatestock")
    void updateProductStockQuantity(@RequestBody OrderCreateDTO orderCreateDTO);
}
