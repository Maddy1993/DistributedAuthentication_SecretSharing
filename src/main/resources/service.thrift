namespace java generated.thrift.impl

enum MessageType {
    PROPOSAL,
    PROMISE,
    PROMISED,
    ACCEPT_REQUEST,
    ACCEPT_RESPONSE,
    READ_RESPONSE,
    READ,
    SUCCESS,
    FAILURE
}

enum OperationType {
    WRITE,
    DELETE,
    GET
}

//Packet structure for client to server communication
struct RequestPacket {
    1:  MessageType         type,
    2:  OperationType       operationType,
    3:  map<string, string> keyValue
}

//Packet structure for server to server communication
struct ServerPacket {
    1: MessageType          type,
    2: string               sequence_number,
    3: OperationType        operationType,
    4: map<string, string>  proposalValue
}

//Interface for client to server communication and server to server communication
service CommunicationService {
    RequestPacket   login(1:string hashedPassword),
    RequestPacket   getValueForKey(1:string key),
    list<string>    getKeys(),
    RequestPacket   storeKeyValue(1:map<string, string> keyValue),
    RequestPacket   deleteKey(1:string  key),
    list<string>    replicaAddresses(),
    MessageType     ping(),
    ServerPacket    acceptProposal(1:ServerPacket packet),
    string          getStoredValue(1:string key)
}