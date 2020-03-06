package jpabook.jpashop.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderItemQueryDto {

    private Long orderId;
    private String itemName;
    private int orderPrice;
    private int count;

    // public OrderItemQueryDto(OrderItem ot) {
    // itemName = ot.getItem().getName();
    // orderPrice = ot.getOrderPrice();
    // count = ot.getCount();
    // }

}