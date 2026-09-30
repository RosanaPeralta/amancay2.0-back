package com.amancay.payment.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface BuyerEmailPort {
    Map<UUID, String> findEmailsByUserId(Set<UUID> userIds);
}
