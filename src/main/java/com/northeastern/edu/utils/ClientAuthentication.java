package com.northeastern.edu.utils;

import generated.thrift.impl.CommunicationService;
import generated.thrift.impl.RequestPacket;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TSocket;

public abstract class ClientAuthentication implements CommunicationService.Iface {
    @Override
    public RequestPacket login(String hashedPassword) throws TException {
        return null;
    }
}