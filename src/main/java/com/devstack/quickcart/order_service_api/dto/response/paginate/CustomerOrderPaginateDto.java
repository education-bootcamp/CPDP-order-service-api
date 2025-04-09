package com.devstack.quickcart.order_service_api.dto.response.paginate;

import com.devstack.quickcart.order_service_api.dto.response.CustomerOrderResponseDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerOrderPaginateDto {
    private long count;
    private List<CustomerOrderResponseDto> dataList;
}
