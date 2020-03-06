package jpabook.jpashop.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDto() {
        List<OrderQueryDto> result = findOrders();
        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });
        return result;
    }

    public List<OrderQueryDto> findAllByDtoOptimization() {
        List<OrderQueryDto> result = findOrders();
        List<Long> orderIds = toOrderIds(result);
        List<OrderItemQueryDto> orderItems = findOrderItemMap(orderIds);
        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(oiqd -> oiqd.getOrderId()));
        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
        return result;
    }

    private List<OrderItemQueryDto> findOrderItemMap(List<Long> orderIds) {
        return em.createQuery(
                "select new jpabook.jpashop.repository.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) from OrderItem oi join oi.item i where oi.order.id in :orderIds",
                OrderItemQueryDto.class).setParameter("orderIds", orderIds).getResultList();
    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream().map(o -> o.getOrderId()).collect(Collectors.toList());
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address) from Order o join o.member m join o.delivery d",
                OrderQueryDto.class).getResultList();
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop.repository.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) from OrderItem oi join oi.item i where oi.order.id = :orderId",
                OrderItemQueryDto.class).setParameter("orderId", orderId).getResultList();
    }

    public List<OrderQueryDto> findAllByDtoFlat() {
        List<OrderFlatDto> flats = em.createQuery(
                "select new jpabook.jpashop.repository.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count) from Order o join o.member m join o.delivery d join o.orderItems oi join oi.item i",
                OrderFlatDto.class).getResultList();
        return flats.stream().collect(Collectors.groupingBy(
                o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(),
                        o.getAddress()),
                        Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()),
                Collectors.toList())))
                .entrySet().stream().map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(Collectors.toList());
    }

}