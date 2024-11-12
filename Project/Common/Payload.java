package Project.Common;
import java.io.Serializable;
import Project.Common.LoggerUtil; //vvh - 11/10/24 Imports LoggerUtil for structured Loggin

public class Payload implements Serializable {
    private static final LoggerUtil logger = LoggerUtil.INSTANCE; //vvh - 11/10/24 LoggerUtil instance for Loggin debug information
    private PayloadType payloadType;
    private long clientId;
    private String message;

    

    public PayloadType getPayloadType() {
        return payloadType;
    }



    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }



    public long getClientId() {
        return clientId;
    }



    public void setClientId(long clientId) {
        this.clientId = clientId;
    }



    public String getMessage() {
        return message;
    }



    public void setMessage(String message) {
        this.message = message;
    }



    @Override
    public String toString(){
        //vvh 11/10/24 Format the payload details into a debug string, including payLoad type, clientID, and message.
        String debugInfo = String.format("Payload[%s] Client Id [%s] Message: [%s]", getPayloadType(), getClientId(), getMessage());
        //vvh - 11/10/24 Log the debug information to mantain structured and configurable Logging
        logger.info(debugInfo);//vvh - 11/10/24 Logs the payload details at the INFO Level
        return debugInfo;//vvh - 11/10/24 Returns the formatted string for external use 
    }
}