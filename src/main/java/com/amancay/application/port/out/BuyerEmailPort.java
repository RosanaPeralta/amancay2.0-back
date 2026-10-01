package com.amancay.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface BuyerEmailPort {
    Map<UUID, String> findEmailsByUserId(Set<UUID> userIds);
}
