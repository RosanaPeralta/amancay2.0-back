package com.amancay.application.port.out;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;

public interface SendOrderCommandPort {
    // Lanza MessagingUnavailableException si el broker no acepta el comando.
    void send(AdvanceOrderStatusCommand command);
}
