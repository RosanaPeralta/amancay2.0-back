package com.amancay.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amancay.application.service.ProductApplicationService;
import com.amancay.application.service.CategoryApplicationService;
import com.amancay.application.service.DiscountApplicationService;
import com.amancay.application.service.UserApplicationService;
import com.amancay.application.service.AddressApplicationService;
import com.amancay.application.service.FavoriteApplicationService;
import com.amancay.application.service.ReviewApplicationService;
import com.amancay.application.service.OrderApplicationService;
import com.amancay.application.service.PaymentApplicationService;
import com.amancay.domain.ports.in.AddressUseCases;
import com.amancay.domain.ports.in.FavoriteUseCases;
import com.amancay.domain.ports.in.ReviewUseCases;
import com.amancay.domain.ports.in.OrderUseCases;
import com.amancay.domain.ports.in.PaymentUseCases;
import com.amancay.domain.ports.in.CategoryUseCases;
import com.amancay.domain.ports.in.DiscountUseCases;
import com.amancay.domain.ports.in.ProductUseCases;
import com.amancay.domain.ports.in.UserUseCases;
import com.amancay.domain.ports.out.CategoryCatalogPort;
import com.amancay.domain.ports.out.DiscountCatalogPort;
import com.amancay.domain.ports.out.DiscountLookupPort;
import com.amancay.domain.ports.out.ProductCatalogPort;
import com.amancay.domain.ports.out.UserAccountPort;
import com.amancay.domain.ports.out.AddressCatalogPort;
import com.amancay.domain.ports.out.AddressUserLockPort;
import com.amancay.domain.ports.out.FavoriteCatalogPort;
import com.amancay.domain.ports.out.FavoriteProductLookupPort;
import com.amancay.domain.ports.out.ReviewCatalogPort;
import com.amancay.domain.ports.out.ReviewPurchasePort;
import com.amancay.domain.ports.out.OrderCatalogPort;
import com.amancay.domain.ports.out.OrderAddressPort;
import com.amancay.domain.ports.out.OrderUserPort;
import com.amancay.domain.ports.out.OrderEventPort;
import com.amancay.domain.ports.out.VariantStockPort;
import com.amancay.domain.ports.out.PaymentCatalogPort;
import com.amancay.domain.ports.out.PaymentOrderPort;
import com.amancay.domain.ports.out.PaymentProcessorPort;
import com.amancay.domain.ports.out.PaymentStockPort;

@Configuration
public class ProductModuleConfiguration {
    @Bean
    public ProductUseCases productUseCases(ProductCatalogPort productCatalog, DiscountLookupPort discountLookup) {
        return new ProductApplicationService(productCatalog, discountLookup);
    }

    @Bean
    public CategoryUseCases categoryUseCases(CategoryCatalogPort categoryCatalog) {
        return new CategoryApplicationService(categoryCatalog);
    }

    @Bean
    public DiscountUseCases discountUseCases(DiscountCatalogPort discountCatalog) {
        return new DiscountApplicationService(discountCatalog);
    }

    @Bean
    public UserUseCases userUseCases(UserAccountPort userAccount) {
        return new UserApplicationService(userAccount);
    }

    @Bean
    public AddressUseCases addressUseCases(AddressCatalogPort addressCatalog, AddressUserLockPort userLock) {
        return new AddressApplicationService(addressCatalog, userLock);
    }

    @Bean
    public FavoriteUseCases favoriteUseCases(FavoriteCatalogPort favoriteCatalog,
            FavoriteProductLookupPort productLookup) {
        return new FavoriteApplicationService(favoriteCatalog, productLookup);
    }

    @Bean
    public ReviewUseCases reviewUseCases(ReviewCatalogPort reviewCatalog, ReviewPurchasePort purchasePort) {
        return new ReviewApplicationService(reviewCatalog, purchasePort);
    }

    @Bean
    public OrderUseCases orderUseCases(OrderCatalogPort orderCatalog, OrderUserPort users, OrderAddressPort addresses,
            VariantStockPort variants, OrderEventPort events) {
        return new OrderApplicationService(orderCatalog, users, addresses, variants, events);
    }

    @Bean
    public PaymentUseCases paymentUseCases(PaymentCatalogPort payments, PaymentOrderPort orders,
            PaymentProcessorPort processors, PaymentStockPort stock) {
        return new PaymentApplicationService(payments, orders, processors, stock);
    }
}