package jpabook.jpashop.repository;

import java.time.LocalDateTime;
import java.util.List;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(of = "orderId")
public class OrderQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    private List<OrderItemQueryDto> orderItems;

    public OrderQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }


    /*
     * public SimpleOrderQueryDto(Order order) { orderId = order.getId(); name =
     * order.getMember().getName(); orderDate = order.getOrderDate(); orderStatus =
     * order.getStatus(); address = order.getDelivery().getAddress(); }
     */

}