package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RatingSummaryTest {

    @Test
    void emptyWhenThereAreNoReviews() {
        RatingSummary result = RatingSummary.from(List.of());

        assertThat(result.average()).isNull();
        assertThat(result.total()).isZero();
        assertThat(result.distribution()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
    }

    @Test
    void computesAverageAndFullDistribution() {
        RatingSummary result = RatingSummary.from(List.of(
                new RatingCount(5, 3L), new RatingCount(4, 1L), new RatingCount(1, 1L)));

        // (5*3 + 4 + 1) / 5 = 4.0
        assertThat(result.average()).isEqualTo(4.0);
        assertThat(result.total()).isEqualTo(5L);
        assertThat(result.distribution()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 1L, 2, 0L, 3, 0L, 4, 1L, 5, 3L));
    }

    @Test
    void roundsAverageToOneDecimal() {
        // (5 + 4 + 4) / 3 = 4.333...
        RatingSummary result = RatingSummary.from(List.of(new RatingCount(5, 1L), new RatingCount(4, 2L)));

        assertThat(result.average()).isEqualTo(4.3);
    }
}
