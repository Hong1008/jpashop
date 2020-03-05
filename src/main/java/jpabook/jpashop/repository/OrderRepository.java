package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order){
        em.persist(order);
    }

    public Order findOne(Long id){
        return em.find(Order.class, id);
    }

    public List<Order> findAll(){
        return em.createQuery("select o from Order o"
                ,Order.class)
                .getResultList();
    }

    public List<Order> findAll(OrderSearch orderSearch){
        return em.createQuery("select o from Order o join o.member m" +
                        " where o.status = :status" +
                        " and m.name like :name"
                ,Order.class)
                .setParameter("name",orderSearch.getMemberName())
                .setParameter("status",orderSearch.getOrderStatus())
                .setMaxResults(10)
                .getResultList();
    }

    public List<Order> findAllWithFetch() {
        return em.createQuery("select o from Order o join fetch o.member join fetch o.delivery"
                ,Order.class)
                .getResultList();
    }

    public List<SimpleOrderQueryDto> findAllWithDtos() {
        return em.createQuery("select " +
                        "new jpabook.jpashop.repository.SimpleOrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                        "from Order o join o.member m join o.delivery d"
                ,SimpleOrderQueryDto.class)
                .getResultList();
    }
}
