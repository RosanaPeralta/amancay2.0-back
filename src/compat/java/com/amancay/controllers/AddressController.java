package com.amancay.controllers;

public class AddressController extends com.amancay.infrastructure.adapters.in.web.AddressController {
    public AddressController(com.amancay.service.AddressService addressService) {
        super(addressService);
    }
}
