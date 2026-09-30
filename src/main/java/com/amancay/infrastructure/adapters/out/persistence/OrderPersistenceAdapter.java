package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.CustomerAddress;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.model.VariantStock;
import com.amancay.domain.ports.out.OrderAddressPort;
import com.amancay.domain.ports.out.OrderCatalogPort;
import com.amancay.domain.ports.out.OrderUserPort;
import com.amancay.domain.ports.out.VariantStockPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.AddressPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.mapper.OrderPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.mapper.UserPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.AddressRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.OrderRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.ProductVariantRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.UserRepository;

@Repository
public class OrderPersistenceAdapter implements OrderCatalogPort, OrderUserPort, OrderAddressPort, VariantStockPort {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;

    public OrderPersistenceAdapter(OrderRepository orderRepository, UserRepository userRepository,
            ProductVariantRepository productVariantRepository, AddressRepository addressRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByUserId(UUID userId) {
        return orderRepository.findByUserId(userId).stream().map(OrderPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findById(UUID id) {
        return orderRepository.findById(id).map(OrderPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public PurchaseOrder save(PurchaseOrder order) {
        com.amancay.entity.Order entity = order.id() == null ? new com.amancay.entity.Order()
                : orderRepository.findById(order.id()).orElseGet(com.amancay.entity.Order::new);
        return OrderPersistenceMapper.toDomain(orderRepository.saveAndFlush(
                OrderPersistenceMapper.toPersistence(order, entity, productVariantRepository)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findUserById(UUID id) {
        return userRepository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAddress> findAddressById(UUID id) {
        return addressRepository.findById(id).map(AddressPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VariantStock> findVariantById(UUID id) {
        return productVariantRepository.findById(id)
                .map(variant -> new VariantStock(variant.getId(), variant.getPrice(), variant.getStockQuantity()));
    }
}