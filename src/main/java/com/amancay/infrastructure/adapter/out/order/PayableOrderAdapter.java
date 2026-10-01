package com.amancay.infrastructure.adapter.out.order;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.in.ChangeOrderStatusUseCase;
import com.amancay.application.port.in.GetOrderQuery;
import com.amancay.application.port.out.PayableOrderPort;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// Traduce entre las ordenes (sus puertos de entrada y su modelo) y lo que necesita
// el cobro de pagos.
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
