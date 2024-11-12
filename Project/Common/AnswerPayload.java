package Project.Common;

public class AnswerPayload extends Payload {
    private String choice; //vvh - 11/11/24 Stores the player's answer choice (e.g., "A", "B", "C", or "D")

    /**
     * vvh - 11/11/24 Constructor to create an AnswerPayload with the specified client ID and answer choice.
     *
     * @param clientId the unique ID of the client sending the answer
     * @param choice the answer choice selected by the player (e.g., "A", "B", "C", or "D")
     */

    public AnswerPayload(long clientId, String choice) {
        setClientId(clientId); //vvh-11/11/24 Set the client ID for this payload
        setPayloadType(PayloadType.ANSWER); //vvh-11/11/24 Specify the payload type as ANSWER
        setMessage("Player answer choice");//vvh-11/11/24 Set a descriptive message for this payload
        this.choice = choice; //vvh - 11/11/24 Store the player's answer choice in this object 
    }

    /**
     * vvh - 11/11/24 Retrieves the answer choice sent by the player.
     *
     * @return the answer choice (e.g., "A", "B", "C", or "D")
     */

    public String getChoice() {
        return choice;
    }

    /**
     * vvh- 11/11/24 Returns a string representation of the AnswerPayload for debugging or logging purposes.
     *
     * @return a formatted string with the client ID and the answer choice
     */

    @Override
    public String toString() {
        return String.format("AnswerPayload[ClientId: %s, Choice: %s]", getClientId(), choice);
    }
}
