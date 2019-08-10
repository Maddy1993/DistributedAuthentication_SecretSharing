namespace java thrift.impl

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

struct RPCPacket {
    1: i32                  type,
    2: string               sequence_number,
    3: map<string, string>  keyValue,
    4: i32                  operationType
}

service RPCPacketService {
    RPCPacket       proposal(1:RPCPacket message),
    list<string>    replicaAddresses()
    MessageType     write(1:map<string, string> value, 2:i32 operationType)
    list<string>    getKeys(),
    string          getValue(1:string key),
    string          getValueFromMemory(1:string key),
    MessageType     ping()
}