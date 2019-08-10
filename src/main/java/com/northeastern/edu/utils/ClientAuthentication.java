package com.northeastern.edu.utils;

import generated.thrift.impl.CommunicationService;
import generated.thrift.impl.RequestPacket;
import org.apache.thrift.TException;

public abstract class ClientAuthentication implements CommunicationService.Iface {
    @Override
    public RequestPacket login(String hashedPassword) throws TException {
        return null;
    }
}