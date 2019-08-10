package com.northeastern.edu.utils;

import generated.thrift.impl.*;

import org.apache.thrift.TException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ServiceHandler extends ClientAuthentication {
    //Logger for the class.
    private static Logger LOGGER = Logger.getLogger(ServiceHandler.class.getName());

    //Variable to hold the data-store of the server.
    //Data-store is represented as key-value pair where key and value strings.
    protected Map<String, String> keyValuePair;

    //FileName to canWriteOrDelete and read data from memory.
    private String memoryObjectFileName = "data";

    //Replica Port Numbers on localhost.
    protected List<Integer> replicaPorts;

    //Host Address.
    protected String hostAddress;

    //Current sequence number.
    protected static LocalDateTime currentSequenceNumber;

    //Constructor for initializing the key-value store.
    public ServiceHandler(List<Integer> replicaPorts, Integer portNumber) throws IOException {

        //Construct file name for the server.
        this.memoryObjectFileName += ":" + portNumber.toString() + ".json";

        //Host Address
        this.hostAddress = "localhost";

        //create connection for the clients.
        this.replicaPorts = replicaPorts;

        //The current sequence number would be set to now() initially.
        currentSequenceNumber = LocalDateTime.now();

        //Load the existing key value store of the server.
        this.keyValuePair = (Map<String, String>) loadMemoryObject(0);
    }

    //Loads the key value data store from memory.
    protected Object loadMemoryObject(int mode) throws IllegalStateException, IOException {
        try {
            FileReader reader = new FileReader(memoryObjectFileName);
            JSONParser jsonParser = new JSONParser();
            return ((Map<String, String>)jsonParser.parse(reader)).get("data");
        } catch (IOException e) {
            String message = "Error loading data from memory: " + e.getMessage();
            LOGGER.severe(message);

            if (mode == 0) {
                new File(memoryObjectFileName).createNewFile();
            } else {}

        } catch (ParseException e) {
            LOGGER.info("File: " + memoryObjectFileName + " is empty.");
        }

        return defaultMemoryObject();
    }

    //Generates the structure of a default memory object
    private Object defaultMemoryObject() {
        Map<String, Object> defaultMemoryObject = new HashMap<>();
        return defaultMemoryObject;
    }

    //Write the learned value to memory
    void writeToMemory(Map<String, String> keyValuePair) {
        try {
            //Creating a map of values to store.
            JSONObject jsonObject = new JSONObject();
            OutputStream writer = new FileOutputStream(memoryObjectFileName);
            jsonObject.put("data", keyValuePair);
            writer.write(jsonObject.toJSONString().getBytes());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            LOGGER.severe("Error while saving the file to memory." + e.getMessage());
        }
    }

    //Definition needs to be provided by child class.
    protected String getValue(String key) throws TException { return "";}

    protected boolean canWriteOrDelete(Map<String, String> keyValue, OperationType write) throws TException { return false;}

    @Override
    public RequestPacket getValueForKey(String key) throws TException {
        RequestPacket response = new RequestPacket();

        String value = getValue(key);
        Map<String, String> responseValue = new HashMap<>();

        if (!value.isEmpty()) {
            response.type = MessageType.SUCCESS;
            responseValue.put(key, value);
            response.keyValue = responseValue;
        } else {
            response.type = MessageType.FAILURE;
            response.keyValue = responseValue;
        }

        return response;
    }

    @Override
    public List<String> getKeys() throws TException {
        try {
            this.keyValuePair = (Map<String, String>) loadMemoryObject(1);
            return new ArrayList<>(keyValuePair.keySet());
        } catch (IOException e) {
            LOGGER.severe("Error loading memory object");
            throw new TException("Error loading memory object: " + e.getMessage());
        }
    }

    @Override
    public RequestPacket storeKeyValue(Map<String, String> keyValue) throws TException {
        RequestPacket response = new RequestPacket();

        if (canWriteOrDelete(keyValue, OperationType.WRITE)) {
            this.keyValuePair.putAll(keyValue);
            writeToMemory(this.keyValuePair);
            response.type = MessageType.SUCCESS;
        } else {
            response.type = MessageType.FAILURE;
        }

        return response;
    }

    @Override
    public RequestPacket deleteKey(String key) throws TException {
        RequestPacket response = new RequestPacket();

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put(key, this.keyValuePair.get(key));
        if (canWriteOrDelete(keyValue, OperationType.DELETE)) {
            this.keyValuePair.remove(key);
            writeToMemory(this.keyValuePair);
            response.type = MessageType.SUCCESS;
        } else {
            response.type = MessageType.FAILURE;
        }

        return response;
    }

    @Override
    public List<String> replicaAddresses() throws TException {
        return new ArrayList<>();
    }

    @Override
    public MessageType ping() throws TException {
        return null;
    }

    @Override
    public ServerPacket acceptProposal(ServerPacket packet) throws TException {
        return new ServerPacket();
    }

    @Override
    public String getStoredValue(String key) throws TException {
        return "";
    }
}
