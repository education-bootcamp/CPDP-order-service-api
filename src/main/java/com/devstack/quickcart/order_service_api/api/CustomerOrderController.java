package com.devstack.quickcart.order_service_api.api;

import com.devstack.quickcart.order_service_api.dto.request.CustomerOrderRequestDto;
import com.devstack.quickcart.order_service_api.service.CustomerOrderService;
import com.devstack.quickcart.order_service_api.util.StandardResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/customer-orders")
@RequiredArgsConstructor
public class CustomerOrderController {
    private final CustomerOrderService customerOrderService;

    @PostMapping("/business")
    public ResponseEntity<StandardResponseDto> create(
            @RequestBody CustomerOrderRequestDto request
            /*auth headers*/) {
        customerOrderService.createOrder(request);
        return new ResponseEntity<>(
                new StandardResponseDto(
                        201,"customer order has been created",null
                ), HttpStatus.CREATED
        );
    }

    @GetMapping("/visitors/find-by-id/{id}")
    public ResponseEntity<StandardResponseDto> findById(@PathVariable String id) {
        return new ResponseEntity<>(
                new StandardResponseDto(
                        200,"customer order details",customerOrderService.findOrderById(id)
                ), HttpStatus.OK
        );
    }

    @PutMapping("/business/update-order/{id}")
    public ResponseEntity<StandardResponseDto> updateOrder(@RequestBody CustomerOrderRequestDto request, @PathVariable String id) {
        customerOrderService.updateOrder(request, id);
        return new ResponseEntity<>(
                new StandardResponseDto(
                        200,"customer order has been updated",null
                ), HttpStatus.OK
        );
    }

    @PutMapping("/business/update-remark/{id}")
    public ResponseEntity<StandardResponseDto> manageRemark(@RequestParam String remark, @PathVariable String id) {
        customerOrderService.manageRemark(remark, id);
        return new ResponseEntity<>(
                new StandardResponseDto(
                        200,"customer order has been updated",null
                ), HttpStatus.OK
        );
    }

    @PutMapping("/business/update-status/{id}")
    public ResponseEntity<StandardResponseDto> manageStatus(@RequestParam String status, @PathVariable String id) {
        customerOrderService.manageStatus(status, id);
        return new ResponseEntity<>(
                new StandardResponseDto(
                        200,"customer order has been updated",null
                ), HttpStatus.OK
        );
    }

    @DeleteMapping("/business/delete-by-id/{id}")
    public ResponseEntity<StandardResponseDto> deleteById(@PathVariable String id) {
        customerOrderService.deleteById(id);
        return new ResponseEntity<>(
                new StandardResponseDto(
                        204,"customer order has been deleted",null
                ), HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/visitors/search-all")
    public ResponseEntity<StandardResponseDto> searchAll(@RequestParam String searchText,@RequestParam int page,@RequestParam int size) {
        return new ResponseEntity<>(
                new StandardResponseDto(
                        200,"customer order list",customerOrderService.searchAll(searchText, page, size)
                ), HttpStatus.OK
        );
    }

}
