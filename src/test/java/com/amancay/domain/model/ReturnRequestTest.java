
package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReturnRequestTest {

    @Test
    void createsReturnRequestWithPendingStatus() {
        UUID orderId = UUID.randomUUID();

        ReturnRequest request = ReturnRequest.create(
                orderId,
                ReturnReason.WRONG_SIZE,
                "El talle me quedó grande");

        assertThat(request.getOrderId()).isEqualTo(orderId);
        assertThat(request.getReason()).isEqualTo(ReturnReason.WRONG_SIZE);
        assertThat(request.getComment()).isEqualTo("El talle me quedó grande");
        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.PENDIENTE);
        assertThat(request.getRequestedAt()).isNotNull();
        assertThat(request.isPending()).isTrue();
        assertThat(request.isResolved()).isFalse();
    }

    @Test
    void normalizesBlankCommentToNull() {
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.OTHER,
                "   ");

        assertThat(request.getComment()).isNull();
    }

    @Test
    void createsReturnRequestWithoutOptionalComment() {
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.DAMAGED,
                null);

        assertThat(request.getComment()).isNull();
        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.PENDIENTE);
    }

    @Test
    void approvesPendingReturnRequest() {
        UUID adminId = UUID.randomUUID();

        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.WRONG_SIZE,
                null);

        request.approve(adminId);

        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.APROBADA);
        assertThat(request.getResolvedBy()).isEqualTo(adminId);
        assertThat(request.getResolvedAt()).isNotNull();
        assertThat(request.isPending()).isFalse();
        assertThat(request.isResolved()).isTrue();
    }

    @Test
    void rejectsPendingReturnRequestWithResolutionNote() {
        UUID adminId = UUID.randomUUID();

        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.OTHER,
                "No era lo esperado");

        request.reject(adminId, "El producto fue utilizado.");

        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.RECHAZADA);
        assertThat(request.getResolvedBy()).isEqualTo(adminId);
        assertThat(request.getResolvedAt()).isNotNull();
        assertThat(request.getResolutionNote())
                .isEqualTo("El producto fue utilizado.");
        assertThat(request.isResolved()).isTrue();
    }

    @Test
    void cannotRejectReturnRequestWithoutResolutionNote() {
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.DAMAGED,
                null);

        assertThatThrownBy(() -> request.reject(UUID.randomUUID(), "   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.PENDIENTE);
        assertThat(request.getResolvedAt()).isNull();
        assertThat(request.getResolvedBy()).isNull();
    }

    @Test
    void cannotResolveReturnRequestMoreThanOnce() {
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.WRONG_PRODUCT,
                null);

        request.approve(UUID.randomUUID());

        assertThatThrownBy(() -> request.reject(
                UUID.randomUUID(),
                "Intento de rechazo"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.APROBADA);
    }

    @Test
    void cannotApproveReturnRequestWithoutAdminId() {
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.OTHER,
                null);

        assertThatThrownBy(() -> request.approve(null))
                .isInstanceOf(NullPointerException.class);

        assertThat(request.getStatus()).isEqualTo(ReturnRequestStatus.PENDIENTE);
        assertThat(request.getResolvedAt()).isNull();
    }

    @Test
    void requiresOrderIdAndReason() {
        assertThatThrownBy(() -> ReturnRequest.create(
                null,
                ReturnReason.OTHER,
                null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> ReturnRequest.create(
                UUID.randomUUID(),
                null,
                null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createsReturnRequestWithControlledTimestamp() {
        Instant requestedAt = Instant.parse("2026-11-10T12:00:00Z");

        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.OTHER,
                null,
                requestedAt);

        assertThat(request.getRequestedAt()).isEqualTo(requestedAt);
    }

    @Test
    void approvesReturnRequestWithControlledTimestamp() {
        Instant resolvedAt = Instant.parse("2026-11-10T13:00:00Z");
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.OTHER,
                null);

        request.approve(UUID.randomUUID(), resolvedAt);

        assertThat(request.getResolvedAt()).isEqualTo(resolvedAt);
    }

    @Test
    void rejectsReturnRequestWithControlledTimestamp() {
        Instant resolvedAt = Instant.parse("2026-11-10T14:00:00Z");
        ReturnRequest request = ReturnRequest.create(
                UUID.randomUUID(),
                ReturnReason.OTHER,
                null);

        request.reject(UUID.randomUUID(), "Motivo del rechazo", resolvedAt);

        assertThat(request.getResolvedAt()).isEqualTo(resolvedAt);
    }

}