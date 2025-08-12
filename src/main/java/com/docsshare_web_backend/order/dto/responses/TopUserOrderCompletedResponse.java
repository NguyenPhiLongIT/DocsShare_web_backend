package com.docsshare_web_backend.order.dto.responses;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUserOrderCompletedResponse {
    private Long userId;
    private String userName;
    private int completedOrderCount;

}
