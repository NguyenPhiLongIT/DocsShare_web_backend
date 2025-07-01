package com.docsshare_web_backend.order.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.order.dto.requests.OrderFilterRequest;
import com.docsshare_web_backend.order.models.Order;
import org.springframework.data.jpa.domain.Specification;

public class OrderFilter {
    public static Specification<Order> filterByRequest(OrderFilterRequest request) {
        return CommonFilter.filter(request, Order.class);
    }
}
