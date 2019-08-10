package com.northeastern.edu.utils;

import generated.thrift.impl.MessageType;
import generated.thrift.impl.OperationType;
import generated.thrift.impl.ServerCommunication;
import generated.thrift.impl.ServerPacket;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class ServerServiceHandler implements ServerCommunication.Iface {

    //Logger for the class.
    private static Logger LOGGER = Logger.getLogger(ServerServiceHandler.class.getName());

    //Replica Connections. Client-Availability mapping
    private Map<ServerCommunication.Client, Boolean> replicas;

    //Replica Port Numbers on localhost.
    private List<Integer> replicaPorts;

    //Host Address.
    private String hostAddress;

    //Current sequence number.
    private static LocalDateTime currentSequenceNumber;

    //Variable to represent the maximum value seen so far.
    private static Map<String, String> valueOfHighestProposal;

    //Flag to represent the promise status.
    private static boolean promiseStatus;

    //Variable representing the agreed upon value.
    private static Map<String, String> agreedValue;

    //Variable representing the agreed upon sequence number.
    private static String agreedProposal;

    //Variable representing the client service handler
    private ClientServiceHandler clientServiceHandler;

    //Constructor to initialize the addresses of server replicas.
    //and sequence number to initiate paxos.
    ServerServiceHandler(List<Integer> replicaPorts, ClientServiceHandler serviceHandler) {

        //Host Address
        this.hostAddress = "localhost";

        //create connection for the clients.
        this.replicaPorts = replicaPorts;

        //The current sequence number would be set to now() initially.
        currentSequenceNumber = LocalDateTime.now();

        //Initialize the copy of client service handler.
        this.clientServiceHandler = serviceHandler;
    }

    //Creates a client connection for a given port.
    private void createConnection(Integer replicaPort) {
        try {
            TTransport transport = new TSocket(this.hostAddress, replicaPort);
            transport.open();

            TProtocol protocol = new TBinaryProtocol(transport);

            ServerCommunication.Client client = new ServerCommunication.Client(protocol);
            if (client.ping() == MessageType.SUCCESS)
                this.replicas.put(client, true);
        } catch (TTransportException e) {
            LOGGER.severe("Error creating connection to the server: " + e.getMessage());
        } catch (TException e) {
            LOGGER.warning("Error creating connection to replica server on port: " + replicaPort);
        }
    }

    //Checks the existence of connection with replicas
    //or creates new ones if one doesn't exist.
    private void checkOrCreateConnection() throws TException {
        //For each replica port.
        if (replicas == null) {
            this.replicas = new HashMap<>();
            for (Integer replicaPort : replicaPorts) {
                createConnection(replicaPort);
            }
        }
        //Ping replicas to verify their availability.
        else {
            for (ServerCommunication.Client replica: this.replicas.keySet()) {
                if (replica.ping() != MessageType.SUCCESS) {
                    this.replicas.put(replica, false);
                } else {
                    this.replicas.put(replica, true);
                }
            }
        }
    }

    //For each of the replica, sends a proposal request.
    private Map<ServerCommunication.Client, ServerPacket> sendProposalToReplicas
                            (Map<String, String> value, OperationType operationType) throws TException {
        //Store responses to proposal sent back by replicas.
        Map<ServerCommunication.Client, ServerPacket> proposalResponses = new HashMap<>();

        for (ServerCommunication.Client replica: this.replicas.keySet()) {
            if (this.replicas.get(replica)) {
                //Construct the proposal message.
                ServerPacket proposal = new ServerPacket();
                proposal.sequence_number = LocalDateTime.now().toString();
                proposal.type = MessageType.PROPOSAL;
                proposal.proposalValue = value;
                proposal.operationType = operationType;

                //Forward the proposal by invoking the call.
                ServerPacket packet = replica.acceptProposal(proposal);
                proposalResponses.put(replica, packet);
            }
        }

        return proposalResponses;
    }

    //If the sequence is below the current highest sequence number,
    //the request is dropped.
    private boolean verifySequenceNumberForProcessing(String sequence_number) {
        return LocalDateTime.parse(sequence_number).isAfter(currentSequenceNumber);
    }

    //Creates a promise response packet based on the whether it has accepted
    //a proposal or current sequence number values
    private ServerPacket generatePromiseResponse(ServerPacket response, ServerPacket proposerData) {

        //Has promised other proposers?
        if (promiseStatus) {
            response.type = MessageType.PROMISE;
            response.sequence_number = agreedProposal;
            response.proposalValue = agreedValue;
        } else {
            //update the agreed value, proposal number and promise status
            agreedValue = proposerData.proposalValue;
            agreedProposal = proposerData.sequence_number;
            promiseStatus = true;

            response.type = MessageType.PROMISE;
            response.sequence_number = proposerData.sequence_number;
            response.proposalValue = proposerData.proposalValue;
        }


        return response;
    }

    //If the proposer receives the requested responses from a majority
    //of the acceptors, then it can issue a proposal with number n
    //and value v, where v is the value of the highest-numbered proposal
    //among the responses, or is any value selected by the proposer if
    //the responders reported no proposals.
    private Map<ServerCommunication.Client, ServerPacket> identifyProposalValue
                            (Map<ServerCommunication.Client, ServerPacket> responses) {
        Iterator<Map.Entry<ServerCommunication.Client, ServerPacket>> entryIterator = responses.entrySet().iterator();

        while (entryIterator.hasNext()){

            Map.Entry<ServerCommunication.Client, ServerPacket> entry = entryIterator.next();

            //Check if there are any proposals from the acceptors. If not,
            //prepare to issue an accept request to the acceptors by removing failed
            //client responses.
            if (entry.getValue().type == MessageType.PROMISE) {
                //When the proposed value is greater than the current highest
                //proposal value, accept the proposal.
                if (currentSequenceNumber.isBefore(LocalDateTime.parse(entry.getValue().sequence_number))) {
                    currentSequenceNumber = LocalDateTime.parse(entry.getValue().sequence_number);
                    valueOfHighestProposal = entry.getValue().proposalValue;
                }
            } else {
                entryIterator.remove();
            }
        }

        return responses;
    }

    //Calculates majority based on the filtered responses and the total number of replicas
    //available for the proposer.
    private boolean calculateMajority(Map<ServerCommunication.Client, ServerPacket> responses) {
        int totalAvailableReplicas = replicas.size();
        int totalResponses = responses.size();

        //Majority is when the total responses has at least 3/4ths of
        //majority.
        return totalResponses >= totalAvailableReplicas * 3/4;
    }

    //Send accept proposals to the promised acceptors.
    private boolean sendAcceptToReplicas
                        (Map<String, String> value,
                         Map<ServerCommunication.Client, ServerPacket> responses,
                         OperationType operationType) throws TException {
        //Loop through the accept responses.
        ServerPacket acceptProposal = new ServerPacket();
        acceptProposal.type = MessageType.ACCEPT_REQUEST;
        acceptProposal.proposalValue = value;
        acceptProposal.sequence_number = currentSequenceNumber.toString();
        acceptProposal.operationType = operationType;


        Iterator<Map.Entry<ServerCommunication.Client, ServerPacket>> entryIterator = responses.entrySet().iterator();
        while (entryIterator.hasNext()){
            Map.Entry<ServerCommunication.Client, ServerPacket> entry = entryIterator.next();

            //If acceptor is available
            if (this.replicas.get(entry.getKey())) {
                ServerPacket packet = entry.getKey().acceptProposal(acceptProposal);

                if (packet.type == MessageType.FAILURE) {
                    entryIterator.remove();
                } else {
                    responses.put(entry.getKey(), packet);
                }
            }
        }

        return calculateMajority(responses);
    }

    //If an acceptor receives an accept request for
    //a proposal numbered n, it accepts the proposal
    //unless it has already responded to a prepare request
    //having a number greater than n.
    private ServerPacket processAcceptProposal(ServerPacket message) {
        //Check if the proposed sequence number
        //is greater than the current sequence number (which represents
        //the sequence number of the latest accepted proposal)
        if (!LocalDateTime.parse(message.sequence_number).isBefore(currentSequenceNumber)) {
            //The acceptor has learned the value successfully.
            this.clientServiceHandler.writeToMemory(message.proposalValue);

            //Reply to the proposer with a success.
            message.type = MessageType.SUCCESS;
        } else {
            message.type = MessageType.FAILURE;
        }

        return message;
    }

    private ServerPacket processProposalRequest(ServerPacket message) {
        boolean canProcess = verifySequenceNumberForProcessing(message.sequence_number);

        //Response packet to construct based on processing.
        ServerPacket response = new ServerPacket();

        //If can't process, return a failure response to proposer
        //to increase efficiency rather wait on time out.
        if(!canProcess) {
            response.type = MessageType.FAILURE;
            return response;
        }

        //If the acceptor receives a prepare message,
        //it responds to the request with a promise not
        //to accept any more proposals numbered less than n
        //and with the highest-numbered proposal (if any)
        //that it has accepted
        return generatePromiseResponse(response, message);
    }

    private Map<String, Integer> getMajorityValue(String key, Map<String, Integer> majorityValue) throws IOException, TException {

        for (ServerCommunication.Client replica : this.replicas.keySet()) {
            //If replica is available
            if (this.replicas.get(replica)) {
                String response = replica.getStoredValue(key);

                //If the value exists, increment its count
                if (majorityValue.containsKey(response)) {
                    int value = majorityValue.get(response);
                    majorityValue.put(response, ++value);
                } else {
                    //Else add an entry.
                    majorityValue.put(response, 1);
                }
            }
        }

        return majorityValue;
    }

    //Initiates the Paxos algorithm to decide if the operation
    //requested can be performed in a distributed fashion.
    boolean canWriteOrDelete(Map<String, String> value, OperationType operationType) throws TException {
        checkOrCreateConnection();
        promiseStatus=false;

        //Initiate proposal to all the replicas.
        Map<ServerCommunication.Client, ServerPacket> responses = sendProposalToReplicas(value, operationType);

        //Prepare to issue accept requests to acceptors.
        responses = identifyProposalValue(responses);

        //Check if the proposer has a majority.
        boolean hasMajority = calculateMajority(responses);

        //If it has majority, then send accept requests to
        //the acceptors.
        return hasMajority
                && sendAcceptToReplicas(value, responses, operationType);
    }

    String getValue(String key) throws TException {
        try {
            checkOrCreateConnection();

            Map<String, Integer> majorityValue = new HashMap<>();
            majorityValue.put(getStoredValue(key), 1);
            //Contact other replicas and get the value.
            majorityValue = getMajorityValue(key, majorityValue);

            //Majority value is
            String currentKey="";
            int maxCount = 0;
            for (String value: majorityValue.keySet()) {
                if (maxCount < majorityValue.get(value)) {
                    maxCount = majorityValue.get(value);
                    currentKey = value;
                }
            }

            return currentKey;
        } catch (IOException e) {
            LOGGER.severe("Error loading memory object");
            throw new TException("Error loading memory object: " + e.getMessage());
        }
    }

    List<String> getAddresses() {
        //For each replica port number
        List<String> ports = new ArrayList<>();
        for (Integer portNum : this.replicaPorts) {
            ports.add(portNum.toString());
        }

        return ports;
    }

    @Override
    public ServerPacket acceptProposal(ServerPacket packet) throws TException {
        if (packet.type == MessageType.ACCEPT_REQUEST) {
            return processAcceptProposal(packet);
        } else if (packet.type == MessageType.PROPOSAL){
            return processProposalRequest(packet);
        }

        return packet;
    }

    @Override
    public MessageType ping() throws TException {
        LOGGER.info("Ping message received");
        return MessageType.SUCCESS;
    }

    @Override
    public String getStoredValue(String key) throws TException {
        //Get the value from the current system.
        return this.clientServiceHandler
                .getValueForKey(key)
                .keyValue.get(key);
    }
}
