package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList; //UI new addition to the code
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Project.Client.Interfaces.*;
import Project.Common.*;
import Project.Common.TextFX.Color;

/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public enum Client {
    INSTANCE;

    {
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private ConcurrentHashMap<Long, ClientPlayer> knownClients = new ConcurrentHashMap<>();
    private ClientPlayer myData;
    private Phase currentPhase = Phase.READY;

    // constants (used to reduce potential types when using them in code)
    private final String COMMAND_CHARACTER = "/";
    private final String CREATE_ROOM = "createroom";
    private final String JOIN_ROOM = "joinroom";
    private final String LIST_ROOMS = "listrooms";
    private final String DISCONNECT = "disconnect";
    private final String LOGOFF = "logoff";
    private final String LOGOUT = "logout";
    private final String SINGLE_SPACE = " ";
    // other constants
    private final String READY = "ready";

    // callback that updates the UI
    private static List<IClientEvents> events = new ArrayList<IClientEvents>();

    public void addCallback(IClientEvents e) {
        events.add(e);
    }

    // needs to be private now that the enum logic is handling this
    private Client() {
        LoggerUtil.INSTANCE.info("Client Created");
        myData = new ClientPlayer();
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            LoggerUtil.INSTANCE.warning("Unknown host", e);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("IOException", e);
        }
        return isConnected();
    }
    
    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @param username
     * @param callback (for triggering UI events)
     * @return true if connection was successful
     */
    public boolean connect(String address, int port, String username, IClientEvents callback) {
        myData.setClientName(username);
        addCallback(callback);
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
            sendClientName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     * @throws IOException
     */
    private boolean processClientCommand(String text) throws IOException {
        if (isConnection(text)) {
            if (myData.getClientName() == null || myData.getClientName().length() == 0) {
                System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
                return true;
            }
            // replaces multiple spaces with a single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            sendClientName();
            return true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            return true;
        } else if (text.startsWith("/name")) {
            myData.setClientName(text.replace("/name", "").trim());
            System.out.println(TextFX.colorize("Set client name to " + myData.getClientName(), Color.CYAN));
            return true;
        } else if (text.equalsIgnoreCase("/users")) {
            // chatroom version
            /*
             * System.out.println( String.join("\n", knownClients.values().stream()
             * .map(c -> String.format("%s(%s)", c.getClientName(), c.getClientId())).toList()));
             */
            // non-chatroom version
            /**
             * System.out.println( String.join("\n", knownClients.values().stream()
             * .map(c -> String.format("%s(%s) %s", c.getClientName(), c.getClientId(), c.isReady()
             * ? "[x]" : "[ ]")) .toList()));
             */
            // updated to show turn status
            System.out
                    .println(String.join("\n",
                            knownClients.values().stream().map(c -> String.format("%s(%s) %s %s", c.getClientName(),
                                    c.getClientId(), c.isReady() ? "[R]" : "[ ]", c.didTakeTurn() ? "[T]" : "[ ]"))
                                    .toList()));
            return true;
        } else { // logic previously from Room.java
            // decided to make this as separate block to separate the core client-side items
            // vs the ones that generally are used after connection and that send requests
            if (text.startsWith(COMMAND_CHARACTER)) {
                boolean wasCommand = false;
                String fullCommand = text.replace(COMMAND_CHARACTER, "");
                String part1 = fullCommand;
                String[] commandParts = part1.split(SINGLE_SPACE, 2);// using limit so spaces in the command value
                                                                     // aren't split
                final String command = commandParts[0];
                final String commandValue = commandParts.length >= 2 ? commandParts[1] : "";
                switch (command) {
                case CREATE_ROOM:
                    sendCreateRoom(commandValue);
                    wasCommand = true;
                    break;
                case JOIN_ROOM:
                    sendJoinRoom(commandValue);
                    wasCommand = true;
                    break;
                case LIST_ROOMS:
                    sendListRooms(commandValue);
                    wasCommand = true;
                    break;
                // Note: these are to disconnect, they're not for changing rooms
                case DISCONNECT:
                case LOGOFF:
                case LOGOUT:
                    sendDisconnect();
                    wasCommand = true;
                    break;
                // others
                case READY:
                    sendReady();
                    wasCommand = true;
                    break;
                case "turn": //Milestone2 compatible
                    sendTurnAction();
                    wasCommand = true;
                    break;
                }
                
                return wasCommand;
            }
        }
        return false;
    }

    public long getMyClientId() {
        return myData.getClientId();
    }

    public void clientSideGameEvent(String str) {
        events.forEach(event -> {
            if (event instanceof IMessageEvents) {
                // Note: using -2 to target GameEventPanel
                ((IMessageEvents) event).onMessageReceive(Constants.GAME_EVENT_CHANNEL, str);
            }
        });
    }

    // send methods to pass data to the ServerThread
    public void sendTurnAction() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.EXAMPLE_TURN);
        send(p);
    }

    /**
     * Sends the client's intent to be ready. Can also be used to toggle the ready
     * state if coded on the server-side
     * 
     * @throws IOException
     */
    public void sendReady() throws IOException {
        ReadyPayload rp = new ReadyPayload();
        rp.setReady(true); // <- technically not needed as we'll use the payload type as a trigger
        send(rp);
    }

    /**
     * Sends a search to the server-side to get a list of potentially matching Rooms
     * 
     * @param roomQuery optional partial match search String
     * @throws IOException
     */
    public void sendListRooms(String roomQuery) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_LIST);
        p.setMessage(roomQuery);
        send(p);
    }

    /**
     * Sends the room name we intend to create
     * 
     * @param room
     * @throws IOException
     */
    public void sendCreateRoom(String room) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_CREATE);
        p.setMessage(room);
        send(p);
    }

    /**
     * Sends the room name we intend to join
     * 
     * @param room
     */
    public void sendJoinRoom(String room) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_JOIN);
        p.setMessage(room);
        try {
            send(p);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send join room payload", e);
        }
    }

    /**
     * Tells the server-side we want to disconnect
     * 
     * @throws IOException
     */
    void sendDisconnect() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        send(p);
    }

    /**
     * Sends desired message over the socket
     * 
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        if (processClientCommand(message)) {
            return;
        }
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        send(p);
    }

    /**
     * Sends chosen client name after socket handshake
     * 
     * @throws IOException
     */
    private void sendClientName() throws IOException {
        if (myData.getClientName() == null || myData.getClientName().length() == 0) {
            System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
            return;
        }
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientName(myData.getClientName());
        send(cp);
    }

    public void sendAnswer(String answer) { //vvh - 11/23/24 Modified Method using try-catch block
        AnswerPayload answerPayload = new AnswerPayload(myData.getClientId(), answer);
        try {
            send(answerPayload);
            // Notify all callbacks about the sent answer
            /*
            for (IClientEvents event : events) {
                event.onAnswerSent(answer);
            }
            */
        } catch (IOException e) { //vvh - 11/23/24 Modified Method
            LoggerUtil.INSTANCE.severe("Failed to send answer payload", e);
        }
    }

    /**
     * Generic send method to send any Payload over the socket (to ServerThread).
     *
     * @param p The payload to send.
     * @throws IOException If sending fails.
     */
    private void send(Payload p) throws IOException {
        try {
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Socket send exception", e);
            throw e;
        }
    }

    public void start() throws IOException {
        LoggerUtil.INSTANCE.info("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject(); // blocking read
                if (fromServer != null) {
                    // System.out.println(fromServer);
                    processPayload(fromServer);
                } else {
                    LoggerUtil.INSTANCE.info("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            LoggerUtil.INSTANCE.severe("Error reading object as specified type: ", cce);
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.INSTANCE.info("Connection dropped", e);
            }
        } finally {
            closeServerConnection();
        }
        LoggerUtil.INSTANCE.info("listenToServer thread stopped");
    }

    /**
     * Listens for keyboard input from the user
     */
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input"); 
            while (isRunning) { // Run until isRunning is false
                String line = si.nextLine();
                LoggerUtil.INSTANCE.severe(
                        "You shouldn't be using terminal input for Milestone 3. Interaction should be done through the UI");
                // vvh - 11/11/24 Check if the input is an answer choice (A, B, C, or D)
                if (line.equalsIgnoreCase("A") || line.equalsIgnoreCase("B") || line.equalsIgnoreCase("C") || line.equalsIgnoreCase("D")) {
                    sendAnswer(line);//vvh - 11/11/24 call sendAnswer to send the selected answer to the server 
                    continue;
                }

                if (!processClientCommand(line)) {
                    if (isConnected()) {
                        sendMessage(line);
                    } else {
                        System.out.println(
                                "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error in listenToInput()", e);
        }
        LoggerUtil.INSTANCE.info("listenToInput thread stopped");
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        LoggerUtil.INSTANCE.info("Client terminated");
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        myData.reset();
        knownClients.clear();
        try {
            if (out != null) {
                LoggerUtil.INSTANCE.info("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing output stream", e);
        }
        try {
            if (in != null) {
                LoggerUtil.INSTANCE.info("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing input stream", e);
        }
        try {
            if (server != null) {
                LoggerUtil.INSTANCE.info("Closing connection");
                server.close();
                LoggerUtil.INSTANCE.info("Closed socket");
            }
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Error closing socket", e);
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Exception from main()", e);
        }
    }

    /**
     * Handles received message from the ServerThread
     * 
     * @param payload
     */
    private void processPayload(Payload payload) {
        try {
            LoggerUtil.INSTANCE.info("Received Payload: " + payload);
            switch (payload.getPayloadType()) {
                case PayloadType.CLIENT_ID: // get id assigned
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    processClientData(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.SYNC_CLIENT: // silent add
                    cp = (ConnectionPayload) payload;
                    processClientSync(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.DISCONNECT: // remove a disconnected client (mostly for the specific message vs leaving
                                             // a room)
                    cp = (ConnectionPayload) payload;
                    processDisconnect(cp.getClientId(), cp.getClientName());
                    break;
                    // note: we want this to cascade
                case PayloadType.ROOM_JOIN: // add/remove client info from known clients
                    cp = (ConnectionPayload) payload;
                    processRoomAction(cp.getClientId(), cp.getClientName(), cp.getMessage(), cp.isConnect());
                    break;
                case PayloadType.ROOM_LIST:
                    RoomResultsPayload rrp = (RoomResultsPayload) payload;
                    processRoomsList(rrp.getRooms(), "List of available rooms"); //vvh 11/23/24 - Modified
                    break;
                case PayloadType.MESSAGE: // displays a received message
                    processMessage(payload.getClientId(), payload.getMessage());
                    break;
                case PayloadType.READY:
                    ReadyPayload rp = (ReadyPayload) payload;
                    processReadyStatus(rp.getClientId(), rp.isReady(), false);
                    break;
                case PayloadType.SYNC_READY:
                    ReadyPayload qrp = (ReadyPayload)payload;
                    processReadyStatus(qrp.getClientId(), qrp.isReady(), true);
                    break;
                case PayloadType.RESET_READY:
                    // note no data necessary as this is just a trigger
                    processResetReady();
                    break;
                case SPECTATE: 
                case NOT_SPECTATE:
                    processSpectatePayload(payload);
                    break;
                case PayloadType.PHASE:
                    processPhase(payload.getMessage());
                    break;
                case PayloadType.TIME://handles a TimerPayload object
                    TimerPayload timerPayload = (TimerPayload) payload;//Casts the payload to a TimerPayload object
                    processCurrentTimer(timerPayload.getTimerType(), timerPayload.getTime());
                    break;
                case PayloadType.EXAMPLE_TURN:
                    ReadyPayload tp = (ReadyPayload) payload;
                    processTurnStatus(tp.getClientId(), tp.isReady());
                    break;
                case ADD_QUESTION: 
                    LoggerUtil.INSTANCE.info("Received ADD_QUESTION payload");//added
                    AddQuestionPayload aqp = (AddQuestionPayload) payload;//added
                    if(!aqp.isQuestion()) processAddQuestionPayload();//added
                    break;
                case AWAY:
                case NOT_AWAY:
                    processAwayPayload(payload);
                    break;
                case CATEGORIES:
                    processCategories(payload);
                    break;
                case SELECT_CATEGORY:
                    processCategorySelection(payload);
                    break;
                case PayloadType.QUESTION: //vvh - 11/11/24 Handles a received QUESTION payload, displaying the question and options to the client
                    QAPayload questionPayload = (QAPayload) payload;
                    processQuestion(
                        questionPayload.getCategory(),
                        questionPayload.getQuestionText(),
                        questionPayload.getAnswerOptions(),
                        questionPayload.getCorrectAnswer()
                    );

                    // Notify all callbacks about the received question
//                    for (QAPayload event : events) {
//                        event.onQuestionReceived(
//                            questionPayload.getCategory(),
//                            questionPayload.getQuestionText(),
//                            questionPayload.getAnswerOptions()
//                        );
//                    }
                    break;
                case PayloadType.POINTS://vvh - 11/11/24 Handles received SCORE payloads, displaying the scores to the client
                    PointsPayload pp = (PointsPayload) payload;
                    processPoints(pp.getClientId(), pp.getPoints());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload, e);
        }
    }

    private void processCategorySelection(Payload payload) {
        events.forEach(event -> {
            if (event instanceof ICategoryEvents) {
                ((ICategoryEvents) event).onCategorySelected(payload.getMessage());
            }
        });
    }

    private void processCategories(Payload payload) {
        List<String> categories = Arrays.stream(payload.getMessage().split("\\|")).toList();
        events.forEach(event -> {
            if (event instanceof ICategoryEvents) {
                ((ICategoryEvents) event).onReceiveCategories(categories);
            }
        });
    }

    private void processSpectatePayload(Payload payload) {
        ClientPlayer cp = knownClients.get(payload.getClientId());
        if (cp == null) {
            LoggerUtil.INSTANCE.severe("Received spectate payload for unknown client id " + payload.getClientId());
            return;
        }
        cp.setSpectating(payload.getPayloadType() == PayloadType.SPECTATE);
        events.forEach(event -> {
            if (event instanceof ISpectateEvents) {
                ((ISpectateEvents) event).onSpectateStatus(payload.getClientId(), payload.getPayloadType() == PayloadType.SPECTATE);
            }
        });
    }

    private void processAwayPayload(Payload payload) {
        Client.INSTANCE.knownClients.get(payload.getClientId()).setAway(payload.getPayloadType() == PayloadType.AWAY);
        events.forEach(event -> {
            if (event instanceof IAwayStatus) {
                ((IAwayStatus) event).onAwayStatus(payload.getClientId(), payload.getPayloadType() == PayloadType.AWAY);
            }
        });
    }

    private void processAddQuestionPayload() {
        events.forEach(event -> {
            if (event instanceof IQuestionEvents) {
                ((IQuestionEvents) event).onAddQuestion();
            }
        });
    }

    private void processQuestion(String category, String questionText, List<String> answerOptions, String correctAnswer) {
        LoggerUtil.INSTANCE.info("Received question: " + questionText);
        events.forEach(event -> {
            if (event instanceof IQuestionEvents) {
                ((IQuestionEvents) event).onQuestionReceived(category, questionText, answerOptions, correctAnswer);
            }
        });
    }

    /**
     * Returns the ClientName of a specific Client by ID.
     * 
     * @param id
     * @return the name, or Room if id is -1, or [Unknown] if failed to find
     */
    public String getClientNameFromId(long id) {
        if (id == ClientPlayer.DEFAULT_CLIENT_ID) {
            return "Room";
        }
        if (knownClients.containsKey(id)) {
            return knownClients.get(id).getClientName();
        }
        return "[Unknown]";
    }

    // payload processors
    private void processPoints(long clientId, int points) {//Processes a points update payload, updating the points for a specific client
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            knownClients.values().forEach(cp -> cp.setPoints(0));// Reset points for all known clients
        }
        else{
            knownClients.get(clientId).setPoints(points);
        }
        events.forEach(event -> {// Notify all registered event listeners of the points update
            if (event instanceof IPointsEvent) {
                ((IPointsEvent) event).onPointsUpdate(clientId, points); // Trigger the onPointsUpdate method, passing the clientId and updated points
            }
        });
    }

    private void processCurrentTimer(TimerType timerType, int time) {//Iterate through all registered event listeners
        events.forEach(event -> {
            if (event instanceof ITimeEvents) {// Check if the event implements the ITimeEvents interface
                ((ITimeEvents) event).onTimerUpdate(timerType, time);// Cast the event to ITimeEvents and call the onTimerUpdate method
            }
        });
    }

    private void processResetTurns() {
        knownClients.values().forEach(cp -> cp.setTakeTurn(false));
        events.forEach(event -> {
            if (event instanceof ITurnEvent) {
                ((ITurnEvent) event).onTookTurn(ClientPlayer.DEFAULT_CLIENT_ID, false);
            }
        });
    }

    private void processTurnStatus(long clientId, boolean didTakeTurn) {
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            processResetTurns();

        } else {
            ClientPlayer cp = knownClients.get(clientId);
            cp.setTakeTurn(didTakeTurn);
            if (didTakeTurn) {
                System.out.println(
                        TextFX.colorize(String.format("%s finished their turn", cp.getClientName()), Color.CYAN));
                events.forEach(event -> {
                    if (event instanceof IMessageEvents) {
                        ((IMessageEvents) event).onMessageReceive(Constants.GAME_EVENT_CHANNEL,
                                String.format("%s[%s] finished their turn", cp.getClientName(), cp.getClientId()));
                    }
                });

            }

        }
        events.forEach(event -> {
            if (event instanceof ITurnEvent) {
                ((ITurnEvent) event).onTookTurn(clientId, didTakeTurn);
            }
        });

    }

    private void processPhase(String phase) {
        currentPhase = Enum.valueOf(Phase.class, phase);
        System.out.println(TextFX.colorize("Current phase is " + currentPhase.name(), Color.YELLOW));
        events.forEach(event -> {
            if (event instanceof IPhaseEvent) {
                ((IPhaseEvent) event).onReceivePhase(currentPhase);
            }
        });
    }

    private void processResetReady() {
        knownClients.values().forEach(cp -> {
            cp.setReady(false);
            cp.setAway(false);
            cp.setSpectating(false);
            events.forEach(event -> {
                if (event instanceof ISpectateEvents) {
                    ((ISpectateEvents) event).onSpectateStatus(cp.getClientId(), false);
                }
            });

            events.forEach(event -> {
                if (event instanceof IAwayStatus) {
                    ((IAwayStatus) event).onAwayStatus(cp.getClientId(), false);
                }
            });
        });

        System.out.println("Ready status reset for everyone");
    }

    private void processReadyStatus(long clientId, boolean isReady, boolean quiet) {
        if (!knownClients.containsKey(clientId)) {
            LoggerUtil.INSTANCE.severe(String.format("Received ready status [%s] for client id %s who is not known",
                    isReady ? "ready" : "not ready", clientId));
            return;
        }
        ClientPlayer cp = knownClients.get(clientId);
        cp.setReady(isReady);
        if (!quiet) {
            System.out.println(String.format("%s[%s] is %s", cp.getClientName(), cp.getClientId(),
                    isReady ? "ready" : "not ready"));
        }
        events.forEach(event -> {
            if (event instanceof IReadyEvent) {
                ((IReadyEvent) event).onReceiveReady(clientId, isReady, quiet);
            }
        });
    }

    private void processRoomsList(List<String> rooms, String message) {
        // invoke onReceiveRoomList callback
        events.forEach(event -> {
            if (event instanceof IRoomEvents) {
                ((IRoomEvents) event).onReceiveRoomList(rooms, message);
            }
        });

        if (rooms == null || rooms.size() == 0) {
            System.out.println(TextFX.colorize("No rooms found matching your query", Color.RED));
            return;
        }
        System.out.println(TextFX.colorize("Room Results:", Color.PURPLE));
        System.out.println(String.join("\n", rooms));
    }

    private void processDisconnect(long clientId, String clientName) {
        // invoke onClientDisconnect callback
        events.forEach(event -> {
            if (event instanceof IConnectionEvents) {
                ((IConnectionEvents) event).onClientDisconnect(clientId, clientName);
            }
        });
        System.out.println(TextFX.colorize(
                String.format("*%s disconnected*", clientId == myData.getClientId() ? "You" : clientName), Color.RED));
        if (clientId == myData.getClientId()) {
            closeServerConnection();
        }
    }

    private void processClientData(long clientId, String clientName) {
        if (myData.getClientId() == ClientPlayer.DEFAULT_CLIENT_ID) {
            myData.setClientId(clientId);
            myData.setClientName(clientName);
            // invoke onReceiveClientId callback
            events.forEach(event -> {
                if (event instanceof IConnectionEvents) {
                    ((IConnectionEvents) event).onReceiveClientId(clientId);
                }
            });
            // knownClients.put(cp.getClientId(), myData);// <-- this is handled later
        }
    }

    private void processMessage(long clientId, String message) {
        String name = knownClients.containsKey(clientId) ? knownClients.get(clientId).getClientName() : "Room";
        System.out.println(TextFX.colorize(String.format("%s: %s", name, message), Color.BLUE));
        // invoke onMessageReceive callback
        events.forEach(event -> {
            if (event instanceof IMessageEvents) {
                ((IMessageEvents) event).onMessageReceive(clientId, message);
            }
        });
    }

    private void processClientSync(long clientId, String clientName) {

        if (!knownClients.containsKey(clientId)) {
            ClientPlayer cd = new ClientPlayer();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            // invoke onSyncClient callback
            events.forEach(event -> {
                if (event instanceof IConnectionEvents) {
                    ((IConnectionEvents) event).onSyncClient(clientId, clientName);
                }
            });
        }
    }

    private void processRoomAction(long clientId, String clientName, String message,boolean isJoin) {
        String roomName = message.split("\\|")[0];
        long ownerId = -1;
        if(message.contains("|")) {
            String tmp = message.split("\\|")[1];
            if (!tmp.equalsIgnoreCase("null")) {
                ownerId = Long.parseLong(message.split("\\|")[1]);
            }
        }
        if (isJoin && !knownClients.containsKey(clientId)) {
            ClientPlayer cd = new ClientPlayer();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            System.out.println(TextFX.colorize(
                    String.format("*%s[%s] joined the Room %s*", clientName, clientId, roomName), Color.GREEN));
            // invoke onRoomJoin callback
            events.forEach(event -> {
                if (event instanceof IRoomEvents) {
                    ((IRoomEvents) event).onRoomAction(clientId, clientName, roomName, isJoin);
                }
            });

            if(ownerId == clientId && ownerId != -1) {
                LoggerUtil.INSTANCE.info("Owner joined room, sending get categories");
                sendGetCategories(clientId, message);
            }
        } else if (!isJoin) {
            ClientPlayer removed = knownClients.remove(clientId);
            if (removed != null) {
                System.out.println(TextFX.colorize(
                        String.format("*%s[%s] left the Room %s*", clientName, clientId, roomName), Color.YELLOW));
                // invoke onRoomJoin callback
                events.forEach(event -> {
                    if (event instanceof IRoomEvents) {
                        ((IRoomEvents) event).onRoomAction(clientId, clientName, roomName, isJoin);
                    }
                });
            }
            // clear our list
            if (clientId == myData.getClientId()) {
                knownClients.clear();
                // invoke onResetUserList()
                events.forEach(event -> {
                    if (event instanceof IConnectionEvents) {
                        ((IConnectionEvents) event).onResetUserList();
                    }
                });
            }
        }
    }
    private void sendGetCategories(long clientId, String room) {
        GetCategoriesPayload gcp = new GetCategoriesPayload();
        gcp.setClientId(clientId);
        gcp.setRoom(room);
        try {
            send(gcp);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send get categories payload", e);
        }
    }
    private void displayQuestion(QAPayload questionPayload) { //vvh - 11/11/24 Displays question details in the console for the user
        System.out.println(TextFX.colorize("Category: " + questionPayload.getCategory(), Color.GREEN));
        System.out.println(TextFX.colorize("Question: " + questionPayload.getQuestionText(), Color.CYAN));  
        List<String> options = questionPayload.getAnswerOptions();
        System.out.println("A. " + options.get(0)); //vvh - 11/11/24 Display option A
        System.out.println("B. " + options.get(1));
        System.out.println("C. " + options.get(2));
        System.out.println("D. " + options.get(3));
        System.out.println("Please type A, B, C, or D to answer.");//vvh - 11/11/24 Prompt for answer
    }

    public void sendAddQuestion() {
        AddQuestionPayload addQuestionPayload = new AddQuestionPayload();
        try {
            send(addQuestionPayload);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send add question payload", e);
        }
    }

    public void sendAddQuestion(String question, String category, String a, String b, String c, String d, String correct) {
        AddQuestionPayload addQuestionPayload = new AddQuestionPayload(question, category, a, b, c, d, correct);
        try {
            send(addQuestionPayload);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send add question payload", e);
        }
    }

    public void sendAwayPayload(PayloadType payloadType) {
        Payload p = new Payload();
        p.setPayloadType(payloadType);
        try {
            send(p);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send away payload", e);
        }
    }

    public boolean isAway() {
        return knownClients.get(myData.getClientId()).isAway;
    }

    public void sendSpectate(PayloadType type) {
        Payload p = new Payload();
        p.setPayloadType(type);
        try {
            send(p);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send spectate payload", e);
        }
    }

    public boolean isSpectating() {
        return knownClients.get(myData.getClientId()) != null && knownClients.get(myData.getClientId()).isSpectating;
    }

    public void sendCategory(String selectedCategory) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.SELECT_CATEGORY);
        p.setMessage(selectedCategory);
        try {
            send(p);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send category payload", e);
        }
    }

    public void fetchCurrentCategory() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.FETCH_CATEGORY);
        try {
            send(p);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Failed to send category fetch payload", e);
        }
    }

    // end payload processors

}