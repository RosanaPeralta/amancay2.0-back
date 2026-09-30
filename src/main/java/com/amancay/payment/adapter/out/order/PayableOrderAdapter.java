package com.amancay.payment.adapter.out.order;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.order.application.port.in.ChangeOrderStatusUseCase;
import com.amancay.order.application.port.in.GetOrderQuery;
import com.amancay.order.domain.model.Order;
import com.amancay.order.domain.model.OrderStatus;
import com.amancay.payment.application.port.out.PayableOrderPort;

// Traduce entre el modulo order (sus puertos de entrada y su modelo) y lo que payment
// necesita. Es la unica clase de payment que conoce a order.
@Component
class PayableOrderAdapter implements PayableOrderPort {

    private final GetOrderQuery getOrderQuery;
    private final ChangeOrderStatusUseCase changeOrderStatusUseCase;

    PayableOrderAdapter(GetOrderQuery getOrderQuery, ChangeOrderStatusUseCase changeOrderStatusUseCase) {
        this.getOrderQuery = getOrderQuery;
        this.changeOrderStatusUseCase = changeOrderStatusUseCase;
    }

    @Override
    public PayableOrder loadForRequester(UUID requesterId, UUID orderId) {
        return toPayableOrder(getOrderQuery.get(requesterId, orderId, null));
    }

    @Override
    public PayableOrder load(UUID orderId) {
        return toPayableOrder(getOrderQuery.getById(orderId));
    }

    @Override
    public List<PayableOrder> loadAll(Collection<UUID> orderIds) {
        return getOrderQuery.getAllById(orderIds).stream().map(this::toPayableOrder).toList();
    }

    @Override
    public void markAsPaid(UUID orderId) {
        changeOrderStatusUseCase.changeStatus(orderId, OrderStatus.EN_PREPARACION);
    }

    private PayableOrder toPayableOrder(Order order) {
        return new PayableOrder(order.getId(), order.getUserId(), order.getTotal(), order.getItems().stream()
                .map(item -> new PayableOrder.Line(item.productVariantId(), item.quantity()))
                .toList());
    }
}
