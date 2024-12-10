package Project.Server;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import Project.Common.*;//vvh-12/09/24 importing common classes 


/**
 * A server-side representation of a single client.
 * This class is more about the data and abstracted communication
 */
public class ServerThread extends BaseServerThread {
    public static final long DEFAULT_CLIENT_ID = -1;
    private Room currentRoom;
    private long clientId;
    private String clientName;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.clientId = ServerThread.DEFAULT_CLIENT_ID;// this is updated later by the server
        this.onInitializationComplete = onInitializationComplete;

    }

    public void setClientName(String name) {
        if (name == null) {
            throw new NullPointerException("Client name can't be null");
        }
        this.clientName = name;
        onInitialized();
    }

    public String getClientName() {
        return clientName;
    }

    public long getClientId() {
        return this.clientId;
    }

    protected Room getCurrentRoom() {
        return this.currentRoom;
    }

    protected void setCurrentRoom(Room room) {
        if (room == null) {
            throw new NullPointerException("Room argument can't be null");
        }
        currentRoom = room;
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this); // Notify server that initialization is complete
    }

    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE.info(String.format("ServerThread[%s(%s)]: %s", getClientName(), getClientId(), message));
    }

    @Override
    protected void cleanup() {
        currentRoom = null;
        super.cleanup();
    }

    @Override
    protected void disconnect() {
        // sendDisconnect(clientId, clientName);
        super.disconnect();
    }

    // handle received message from the Client
    @Override
    protected void processPayload(Payload payload) {
        try {
            switch (payload.getPayloadType()) {
                case CLIENT_CONNECT:
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    setClientName(cp.getClientName());
                    break;
                case MESSAGE:
                    currentRoom.sendMessage(this, payload.getMessage());
                    break;
                case ROOM_CREATE:
                    currentRoom.handleCreateRoom(this, payload.getMessage());
                    break;
                case ADD_QUESTION://vvh-12/09/24 handle adding question to the game room
                    currentRoom.handleAddQuestion(this, payload);
                    break;
                case SPECTATE://vvh-12/09/24 handles the spectate action
                case NOT_SPECTATE://vvh-12/09/24 handle stop spectating status 
                    currentRoom.handleSpectate(this, payload);
                    try {
                        //vvh-12/09/24 cast to GameRoom as the subclass will handle all Game logic
                        ((GameRoom) currentRoom).handleReadySpec(this);
                    } catch (Exception e) {
                        sendMessage("You must be in a GameRoom to do the ready check");
                    }
                    break;
                case ROOM_JOIN:
                    currentRoom.handleJoinRoom(this, payload.getMessage());
                    break;
                case ROOM_LIST:
                    currentRoom.handleListRooms(this, payload.getMessage());
                    break;
                case DISCONNECT:
                    currentRoom.disconnect(this);
                    break;
                case SELECT_CATEGORY://vvh-12/09/24 handle selecting a category in the game room 
                    currentRoom.handleSelectCategory(this, payload);
                    break;
                case GET_CATEGORIES://vvh-12/09/24 handle fetching a list of categories from the game room
                    currentRoom.handleGetCategories(this);
                    break;
                case AWAY://vvh-12/09/24 handle setting the client as away
                case NOT_AWAY://vvh-12/09/24 handle setting the client as not away
                    handleAwayPayload(this, payload);//vvh-12/09/24 process the away status change payload
                    break; 
                case FETCH_CATEGORY://vvh-12/09/24 handle fetching the currently selected category 
                    currentRoom.handleFetchCategory(this, payload);
                    break;
                case READY:
                    // no data needed as the intent will be used as the trigger
                    try {
                        // cast to GameRoom as the subclass will handle all Game logic
                        ((GameRoom) currentRoom).handleReady(this);
                    } catch (Exception e) {
                        sendMessage("You must be in a GameRoom to do the ready check");
                    }
                    break;
                case ANSWER: // vvh - 11/10/24 Handle the answer payload
                    if (payload instanceof AnswerPayload) {
                        AnswerPayload answerPayload = (AnswerPayload) payload;
                        String choice = answerPayload.getChoice(); // vvh - 11/11/24 Get the answer choice
                        ((GameRoom) currentRoom).processAnswer(getClientId(), choice); // vvh -11/11/24 Process the answer
                    } else {
                        LoggerUtil.INSTANCE.severe("Payload is not of type AnswerPayload.");
                    }
                    break;
                case EXAMPLE_TURN:
                    try {
                        // cast to GameRoom as the subclass will handle all Game logic
                        ((GameRoom) currentRoom).handleTurn(this);
                    } catch (Exception e) {
                        sendMessage("You must be in a GameRoom to do the example turn");
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload, e);

        }
    }

    // send methods specific to non-chatroom
    /**
     * Syncs a specific client's points
     * 
     * @param clientId
     * @param points
     * @return
     */
    public boolean sendPointsUpdate(long clientId, int points) {
        PointsPayload rp = new PointsPayload();
        rp.setPoints(points);
        rp.setClientId(clientId);
        return send(rp);
    }

    /**
     * Syncs the current time of a specific TimerType
     * 
     * @param timerType
     * @param time
     * @return
     */
    public boolean sendCurrentTime(TimerType timerType, int time) {
        TimerPayload tp = new TimerPayload();
        tp.setTime(time);
        tp.setTimerType(timerType);
        return send(tp);
    }

    /**
     * Sends a message as a GAME_EVENT for non-chat UI
     * 
     * @param str
     * @return
     */
    public boolean sendGameEvent(String str) {
        return sendMessage(Constants.GAME_EVENT_CHANNEL, str);
    }

    /**
     * Syncs a specific client's turn status
     * 
     * @param clientId
     * @param didTakeTurn
     * @return
     */
    public boolean sendTurnStatus(long clientId, boolean didTakeTurn) {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.EXAMPLE_TURN);
        rp.setReady(didTakeTurn);
        rp.setClientId(clientId);
        return send(rp);
    }

    /**
     * Syncs the currnet phase to the client
     * 
     * @param phase
     * @return
     */
    public boolean sendCurrentPhase(Phase phase) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PHASE);
        p.setMessage(phase.name());
        return send(p);
    }

    /**
     * Sends a trigger to have the client-side reset their list of READY state
     * 
     * @return
     */
    public boolean sendResetReady() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_READY);
        return send(rp);
    }

    public boolean sendReadyStatus(long clientId, boolean isReady) {
        return sendReadyStatus(clientId, isReady, false);
    }
    
    /**
     * Sync ready status of client id
     * @param clientId who
     * @param isReady ready or not
     * @param quiet silently mark ready
     * @return
     */
    public boolean sendReadyStatus(long clientId, boolean isReady, boolean quiet){
        ReadyPayload rp = new ReadyPayload();
        rp.setClientId(clientId);
        rp.setReady(isReady);
        if(quiet){
            rp.setPayloadType(PayloadType.SYNC_READY);
        }
        return send(rp);
    }
    // send methods to pass data back to the Client

    public boolean sendRooms(List<String> rooms) {
        RoomResultsPayload rrp = new RoomResultsPayload();
        rrp.setRooms(rooms);
        return send(rrp);
    }

    public boolean sendClientSync(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        cp.setConnect(true);
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        return send(cp);
    }
    
    //vvh -11/10/24 method to send a trivia question to the client 
    public boolean sendQuestion(QAPayload question) {
        return send(question);
    }

    /**
     * Overload of sendMessage used for server-side generated messages
     * 
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(String message) {
        return sendMessage(ServerThread.DEFAULT_CLIENT_ID, message);
    }

    /**
     * Sends a message with the author/source identifier
     * 
     * @param senderId
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(long senderId, String message) {
        Payload p = new Payload();
        p.setClientId(senderId);
        p.setMessage(message);
        p.setPayloadType(PayloadType.MESSAGE);
        return send(p);
    }

    /**
     * Tells the client information about a client joining/leaving a room.
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @param message       the room
     * @param isJoin     true for join, false for leaivng
     * @return success of sending the payload
     */
    public boolean sendRoomAction(long clientId, String clientName, String room, boolean isJoin) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.ROOM_JOIN);
        cp.setConnect(isJoin); // <-- determine if join or leave
        cp.setMessage(room+"|"+Server.INSTANCE.roomsByOwner.get(room));
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Tells the client information about a disconnect (similar to leaving a room)
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @return success of sending the payload
     */
    public boolean sendDisconnect(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        cp.setConnect(false);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Sends (and sets) this client their id (typically when they first connect)
     * 
     * @param clientId
     * @return success of sending the payload
     */
    public boolean sendClientId(long clientId) {
        this.clientId = clientId;
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.CLIENT_ID);
        cp.setConnect(true);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }
//vvh-12/09/24 Method to send an "Add Question" request to the client
    public void sendAddQuestion(long clientId) {
        AddQuestionPayload aqp = new AddQuestionPayload();
        aqp.setClientId(clientId); //vvh-12/09/24 Set the client ID for the payload
        aqp.setPayloadType(PayloadType.ADD_QUESTION);
        send(aqp);//vvh-12/09/24 Send the payload to the client
    }
//vvh-12/09/24 Method to handle adding a question in the game room
    public void addQuestion(long clientId, Payload payload) {
        ((GameRoom)currentRoom).addQuestion(clientId, payload); //vvh-12/09/24 Forward the payload to the current game room's addQuestion method
    }
//vvh-12/09/24 Method to handle the "away" status change for a client
    public void handleAwayPayload(ServerThread serverThread, Payload payload) {
        long clientId = serverThread.getClientId();
        boolean isAway = payload.getPayloadType() == PayloadType.AWAY;
        ((GameRoom)currentRoom).handleAway(clientId, isAway);
    }
//vvh-12/09/24 Method to handle the "spectate" action for a client
    public void spectate(long clientId, Payload payload) {
        ((GameRoom)currentRoom).spectate(clientId, payload);
    }
//vvh-12/09/24  Method to send the list of categories to a client
    public void sendCategories(long clientId) {
        ((GameRoom)currentRoom).sendCategories(clientId); //vvh-12/09/24 Delegate sending categories to the current game room
    }
//vvh-12/09/24 Method to send the selected category to a client
    public void sendCategory(long clientId, String currentCategory) {
        ((GameRoom)currentRoom).sendCategory(clientId, currentCategory);
    }

    // end send methods
}