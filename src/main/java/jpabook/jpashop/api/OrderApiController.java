package jpabook.jpashop.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderFlatDto;
import jpabook.jpashop.repository.OrderQueryDto;
import jpabook.jpashop.repository.OrderQueryRepository;
import jpabook.jpashop.repository.OrderRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    //엔티티를 조회해서 그대로 반환
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1(){
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(ot -> ot.getItem().getName());
        }
        return orders;
    }   

    //엔티티 조회 후 DTO로 변환
    /**
     * 지연 로딩으로 너무 많은 SQL 실행
        SQL 실행 수
        order 1번
        member , address N번(order 조회 수 만큼)
        orderItem N번(order 조회 수 만큼)
        item N번(orderItem 조회 수 만큼)

        참고: 지연 로딩은 영속성 컨텍스트에 있으면 영속성 컨텍스트에 있는 엔티티를 사용하고 없으면 SQL을 실
        행한다. 따라서 같은 영속성 컨텍스트에서 이미 로딩한 회원 엔티티를 추가로 조회하면 SQL을 실행하지 않
        는다.
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2(){
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> orderDtos = orders.stream()
                                    .map(o -> new OrderDto(o))
                                    .collect(Collectors.toList());
        return orderDtos;
    }

    //페치 조인으로 쿼리 수 최적화
    /**
     * 페치 조인으로 SQL이 1번만 실행됨
        distinct 를 사용한 이유는 1대다 조인이 있으므로 데이터베이스 row가 증가한다. 그 결과 같은 order
        엔티티의 조회 수도 증가하게 된다. JPA의 distinct는 SQL에 distinct를 추가하고, 더해서 같은 엔티티가
        조회되면, 애플리케이션에서 중복을 걸러준다. 이 예에서 order가 컬렉션 페치 조인 때문에 중복 조회 되는
        것을 막아준다.
        단점
        페이징 불가능
        > 참고: 컬렉션 페치 조인을 사용하면 페이징이 불가능하다. 하이버네이트는 경고 로그를 남기면서 모든 데이
        터를 DB에서 읽어오고, 메모리에서 페이징 해버린다(매우 위험하다). 자세한 내용은 자바 ORM 표준 JPA
        프로그래밍의 페치 조인 부분을 참고하자.
        > 참고: 컬렉션 페치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 데이터가
        부정합하게 조회될 수 있다. 자세한 내용은 자바 ORM 표준 JPA 프로그래밍을 참고하자.
     * @return
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3(){
        List<Order> orders = orderRepository.findAllWithFetchDistinct();
        List<OrderDto> orderDtos = orders.stream()
                                    .map(o -> new OrderDto(o))
                                    .collect(Collectors.toList());
        return orderDtos;
    }

    /**
     * 컬렉션 페이징과 한계 돌파: V3.1
        컬렉션은 페치 조인시 페이징이 불가능
        ToOne 관계는 페치 조인으로 쿼리 수 최적화
        컬렉션은 페치 조인 대신에 지연 로딩을 유지하고, hibernate.default_batch_fetch_size ,
        @BatchSize 로 최적화

        장점
        쿼리 호출 수가 1 + N 1 + 1 로 최적화 된다.
        조인보다 DB 데이터 전송량이 최적화 된다. (Order와 OrderItem을 조인하면 Order가
        OrderItem 만큼 중복해서 조회된다. 이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.)
        페치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
        컬렉션 페치 조인은 페이징이 불가능 하지만 이 방법은 페이징이 가능하다.
        결론    
        ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다. 따라서 ToOne 관계는 페치조인으로 쿼
        리 수를 줄이고 해결하고, 나머지는 hibernate.default_batch_fetch_size 로 최적화 하자.
     * @return
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_1(){
        List<Order> orders = orderRepository.findAllWithFetchBatch();
        List<OrderDto> orderDtos = orders.stream()
                                    .map(o -> new OrderDto(o))
                                    .collect(Collectors.toList());
        return orderDtos;
    }

    /**
     * JPA에서 DTO를 직접 조회
     * 
     * Query: 루트 1번, 컬렉션 N 번 실행
        ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
        이런 방식을 선택한 이유는 다음과 같다.
        ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
        ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
        row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고, ToMany 관계
        는 최적화 하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
     * @return
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
        List<OrderQueryDto> orders = orderQueryRepository.findOrderQueryDto();
        return orders;
    }

    //컬렉션 조회 최적화 - 일대다 관계인 컬렉션은 IN 절을 활용해서 메모리에 미리 조회해서 최적화
    /**
     * Query: 루트 1번, 컬렉션 1번
        ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을 한꺼번
        에 조회
        MAP을 사용해서 매칭 성능 향상(O(1))
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5(){
        List<OrderQueryDto> orders = orderQueryRepository.findAllByDtoOptimization();
        return orders;
    }

    //플랫 데이터 최적화 - JOIN 결과를 그대로 조회 후 애플리케이션에서 원하는 모양으로 직접 변환
    /**
     * Query: 1번
        단점
        쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가되
        므로 상황에 따라 V5 보다 더 느릴 수 도 있다.
        애플리케이션에서 추가 작업이 크다.
        페이징 불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6(){
        List<OrderQueryDto> orders = orderQueryRepository.findAllByDtoFlat();
        return orders;
    }

    @Data
    static class OrderDto{
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order){
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream().map(ot -> new OrderItemDto(ot)).collect(Collectors.toList());
        }
    }

    @Data
    static class OrderItemDto{

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem ot) {
            itemName = ot.getItem().getName();
            orderPrice = ot.getOrderPrice();
            count = ot.getCount();
        }

    }
}