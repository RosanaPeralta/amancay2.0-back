package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.amancay.dto.CreateOrderRequest;
import com.amancay.dto.OrderDto;
import com.amancay.entity.Address;
import com.amancay.entity.Order;
import com.amancay.entity.OrderStatus;
import com.amancay.entity.ProductVariant;
import com.amancay.entity.Role;
import com.amancay.entity.User;
import com.amancay.exceptions.InsufficientStockException;
import com.amancay.repository.AddressRepository;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.ProductVariantRepository;
import com.amancay.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private AddressRepository addressRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, userRepository, productVariantRepository, addressRepository,
                null);
    }

    @Test
    void createOrderDecrementsStockOfEachItem() {
        UUID buyerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        ProductVariant variant = variant(new BigDecimal("50.00"));
        stubAddress(addressId, buyerId);
        when(productVariantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));
        when(productVariantRepository.decrementStock(variant.getId(), 2)).thenReturn(1);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.createOrder(buyerId, request(addressId, variant.getId(), 2));

        assertThat(result.userId()).isEqualTo(buyerId);
        assertThat(result.total()).isEqualByComparingTo("100.00");
        verify(productVariantRepository).decrementStock(variant.getId(), 2);
    }

    @Test
    void createOrderFailsWithoutSavingWhenStockIsInsufficient() {
        UUID buyerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        ProductVariant variant = variant(new BigDecimal("50.00"));
        stubAddress(addressId, buyerId);
        when(productVariantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));
        when(productVariantRepository.decrementStock(variant.getId(), 5)).thenReturn(0);

        assertThatThrownBy(() -> orderService.createOrder(buyerId, request(addressId, variant.getId(), 5)))
                .isInstanceOf(InsufficientStockException.class);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void buyerListsOnlyOwnOrders() {
        UUID buyerId = UUID.randomUUID();
        User buyer = user(buyerId, Role.BUYER);
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(orderRepository.findByUserId(eq(buyerId)))
                .thenReturn(List.of(order(buyerId, OrderStatus.CREADO)));

        List<OrderDto> result = orderService.listOrders(buyerId, buyerId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userId()).isEqualTo(buyerId);
    }

    @Test
    void adminCanListAnotherUsersOrders() {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        User admin = user(adminId, Role.ADMIN);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(orderRepository.findByUserId(eq(targetUserId)))
                .thenReturn(List.of(order(targetUserId, OrderStatus.EN_PREPARACION)));

        List<OrderDto> result = orderService.listOrders(adminId, targetUserId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userId()).isEqualTo(targetUserId);
    }

    @Test
    void buyerCannotListAnotherUsersOrders() {
        UUID buyerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User buyer = user(buyerId, Role.BUYER);
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> orderService.listOrders(buyerId, otherUserId))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void stubAddress(UUID addressId, UUID userId) {
        Address address = Address.create(userId, "Calle", 1, null, "Ciudad", null, "Pais", null);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
    }

    private ProductVariant variant(BigDecimal price) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setPrice(price);
        return variant;
    }

    private CreateOrderRequest request(UUID addressId, UUID variantId, int quantity) {
        return new CreateOrderRequest(addressId, List.of(new CreateOrderRequest.ItemRequest(variantId, quantity)));
    }

    private User user(UUID id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@amancay.com");
        user.setName("User " + id);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Order order(UUID userId, OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUserId(userId);
        order.setStatus(status);
        order.setSubtotal(new BigDecimal("100.00"));
        order.setShippingCost(new BigDecimal("10.00"));
        order.setTotal(new BigDecimal("110.00"));
        return order;
    }
}
