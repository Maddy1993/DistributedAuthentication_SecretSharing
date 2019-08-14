package com.northeastern.edu.utils;

import com.northeastern.edu.secretSharing.Key;
import com.northeastern.edu.secretSharing.SecretSharing;
import generated.thrift.impl.CommunicationService;
import generated.thrift.impl.RequestPacket;
import org.apache.thrift.TException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ClientAuthentication implements CommunicationService.Iface {

    //Secret-Sharing Keys for client.
    protected Map<String, List<Key>> clientKeys;

    protected ClientAuthentication() {
        clientKeys = new HashMap<>();
    }

    @Override
    public RequestPacket login(String hashedPassword, String clientAddress) throws TException {

        //Using the password, construct the key set.
        List<Key> keys = SecretSharing.preparation(hashedPassword);

        return null;
    }
}