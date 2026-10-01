package com.amancay.application.port.in;

import com.amancay.domain.model.Review;

// authorName es users.name tal cual; puede ser null si el usuario nunca lo cargo. Nunca se
// expone el email.
public record ReviewWithAuthor(Review review, String authorName) {
}
