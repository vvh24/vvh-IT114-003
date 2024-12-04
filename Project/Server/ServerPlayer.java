package Project.Server;

import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.QAPayload; // Import QAPayload for sending questions
import Project.Common.Payload; // Import Payload for sending general messages
import Project.Common.PayloadType; //vvh - 11/10/24 Ensure PayLoadType is imported 
import Project.Common.LoggerUtil;
import Project.Common.TimerType;

import java.util.List;

/**
 * Server-only data about a player
 * Added in ReadyCheck lesson/branch for non-chatroom projects.
 * If chatroom projects want to follow this design update the following in this lesson:
 * Player class renamed to User
 * clientPlayer class renamed to ClientUser (or the original ClientData)
 * ServerPlayer class renamed to ServerUser
 */
public class ServerPlayer extends Player{
    private ServerThread client; // reference to wrapped ServerThread
    private int score = 0;//vvh - 11/10/24 Holds the player's current score, starting at 0

    public ServerPlayer(ServerThread clientToWrap){
        client = clientToWrap;
        setClientId(client.getClientId());
    }
    /**
     * Used only for passing the ServerThread to the base class of Room.
     * Favor creating wrapper methods instead of interacting with this directly.
     * @return ServerThread reference
     */
    public ServerThread getServerThread(){
        return client;
    }

    // add any wrapper methods to call on the ServerThread
    // don't used the exposed full ServerThread object
    public boolean sendCurrentTime(TimerType timerType, int time) {
        return client.sendCurrentTime(timerType, time);
    }

    public boolean sendPointsUpdate(long clientId, int points) {
        return client.sendPointsUpdate(clientId, points);
    }

    public boolean sendTurnStatus(long clientId, boolean didTakeTurn) {
        return client.sendTurnStatus(clientId, didTakeTurn);
    }

    public boolean sendReadyStatus(long clientId, boolean isReady, boolean quiet){
        return client.sendReadyStatus(clientId, isReady, quiet);
    }

    public boolean sendReadyStatus(long clientId, boolean isReady){
       return client.sendReadyStatus(clientId, isReady);//vvh - 11/11/24 Sends the ready status with default non-quiet setting
    }

    public boolean sendResetReady(){
        return client.sendResetReady(); //vvh - 11/11/24 Sends a reset-ready signal to the client
    }

    public boolean sendCurrentPhase(Phase phase){
        return client.sendCurrentPhase(phase);
    }

    public boolean sendGameEvent(String message){
        return client.sendGameEvent(message);
    }
    //vvh - 11/10/24 Method to send a trivia question to the client
    public boolean sendQuestion(QAPayload question) {
        return client.send(question); //vvh - 11/11/24 Uses ServerThread's send method to deliver the question payload
    }

    public String getClientName() {
    return client.getClientName(); // vvh - 11/11/24 Retrieves the client name associated with this player
    }

    public int getScore() {
    return score; // vvh - 11/11/24 Returns the player's current score
    }   

    public void resetScore() { //vvh - 11/11/24 Resets the player's score to 0
        score = 0;
    }

    public void incrementScore(int points) {
        score += points; //vvh - 11/11/24 // Adds the specified number of points to the player's score
        LoggerUtil.INSTANCE.info("Incremented score for " + getClientName() + ". New score: " + score);
    }

    // vvh - 11/11/24 method to send a text message to the client
    public boolean sendMessage(String message) {
        Payload payload = new Payload();//vvh 11/11/24 Creates a new Payload to encapsulate the message
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.MESSAGE); //vvh - 11/11/24  Corrected reference to PayloadType
        return client.send(payload); // vvh - 11/11/24 Corrected to use 'payload' instead of 'p'
    }

    public void sendAwayStatus(long clientId, boolean isAway) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(isAway ? PayloadType.AWAY : PayloadType.NOT_AWAY);
        client.send(payload);
    }

    public void sendSpectateStatus(long clientId, boolean isSpectating) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(isSpectating ? PayloadType.SPECTATE : PayloadType.NOT_SPECTATE);
        client.send(payload);
    }

    public void sendCategories(List<String> categories) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.CATEGORIES);
        payload.setMessage(String.join("|", categories));
        client.send(payload);
    }

    public void sendCategory(String selectedCategory) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.SELECT_CATEGORY);
        payload.setMessage(selectedCategory);
        client.send(payload);
    }
}