package com.amancay.exceptions;

public class AddressLimitReachedException extends RuntimeException {
    public AddressLimitReachedException(int max) {
        super("A user can have at most " + max + " addresses");
    }
}
