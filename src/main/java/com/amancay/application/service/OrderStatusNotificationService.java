package com.amancay.application.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.DescribeOrderItemsQuery;
import com.amancay.application.port.in.NotifyOrderStatusChangeUseCase;
import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.application.port.out.MailContent;
import com.amancay.application.port.out.MailSenderPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.service.notification.OrderStatusMail;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.BuyerEmailNotFoundException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.domain.model.OrderStatus;

// Elige el mail (Strategy) segun el estado al que paso el pedido y se lo manda al comprador.
@Service
public class OrderStatusNotificationService implements NotifyOrderStatusChangeUseCase {

    private final Map<OrderStatus, OrderStatusMail> mailsByStatus = new EnumMap<>(OrderStatus.class);
    private final OrderRepositoryPort orderRepository;
    private final DescribeOrderItemsQuery describeOrderItems;
    private final BuyerEmailPort buyerEmailPort;
    private final MailSenderPort mailSender;

    public OrderStatusNotificationService(List<OrderStatusMail> mails, OrderRepositoryPort orderRepository,
            DescribeOrderItemsQuery describeOrderItems, BuyerEmailPort buyerEmailPort, MailSenderPort mailSender) {
        for (OrderStatusMail mail : mails) {
            if (mailsByStatus.putIfAbsent(mail.status(), mail) != null) {
                throw new IllegalStateException("More than one mail for status " + mail.status());
            }
        }
        this.orderRepository = orderRepository;
        this.describeOrderItems = describeOrderItems;
        this.buyerEmailPort = buyerEmailPort;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean notify(OrderStatusChangedEvent event) {
        OrderStatusMail mail = mailsByStatus.get(event.newStatus());
        if (mail == null) {
            return false;
        }
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
        String buyerEmail = buyerEmailPort.findEmailsByUserId(Set.of(order.getUserId())).get(order.getUserId());
        if (buyerEmail == null) {
            throw new BuyerEmailNotFoundException(order.getUserId());
        }
        Map<UUID, OrderItemProduct> products = describeOrderItems.describe(List.of(order));
        MailContent content = mail.build(order, products);
        mailSender.send(buyerEmail, content);
        return true;
    }
}
