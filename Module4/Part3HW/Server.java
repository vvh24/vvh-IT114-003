package Module4.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;//vvh-10/07/24 - imports the Collection class
import java.util.List;//vvh-10/07/24 - imports the List interface
import java.util.ArrayList;//vvh-10/07/24 - imports the ArrayList class
import java.util.Collections;//vvh-10/07/24 - imports the Collections class to use shuffle
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private int port = 3000;
    // connected clients
    // Use ConcurrentHashMap for thread-safe client management
    private final ConcurrentHashMap<Long, ServerThread> connectedClients = new ConcurrentHashMap<>();
    private boolean isRunning = true;

    private void start(int port) {
        this.port = port;
        // server listening
        System.out.println("Listening on port " + this.port);
        // Simplified client connection loop
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (isRunning) {
                System.out.println("Waiting for next client");
                Socket incomingClient = serverSocket.accept(); // blocking action, waits for a client connection
                System.out.println("Client connected");
                // wrap socket in a ServerThread, pass a callback to notify the Server they're initialized
                ServerThread sClient = new ServerThread(incomingClient, this, this::onClientInitialized);
                // start the thread (typically an external entity manages the lifecycle and we
                // don't have the thread start itself)
                sClient.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("Closing server socket");
        }
    }
    /**
     * Callback passed to ServerThread to inform Server they're ready to receive data
     * @param sClient
     */
    private void onClientInitialized(ServerThread sClient) {
        // add to connected clients list
        connectedClients.put(sClient.getClientId(), sClient);
        relay(String.format("*User[%s] connected*", sClient.getClientId()), null);
    }
    /**
     * Takes a ServerThread and removes them from the Server
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param client
     */
    protected synchronized void disconnect(ServerThread client) {
        long id = client.getClientId();
        client.disconnect();
        connectedClients.remove(id);
        // Improved logging with user ID
        relay("User[" + id + "] disconnected", null);
    }

    /**
     * Relays the message from the sender to all connectedClients
     * Internally calls processCommand and evaluates as necessary.
     * Note: Clients that fail to receive a message get removed from
     * connectedClients.
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param message
     * @param sender ServerThread (client) sending the message or null if it's a server-generated message
     */
    protected synchronized void relay(String message, ServerThread sender) {
        if (sender != null && processCommand(message, sender)) {

            return;
        }
        // let's temporarily use the thread id as the client identifier to
        // show in all client's chat. This isn't good practice since it's subject to
        // change as clients connect/disconnect
        // Note: any desired changes to the message must be done before this line
        String senderString = sender == null ? "Server" : String.format("User[%s]", sender.getClientId());
        final String formattedMessage = String.format("%s: %s", senderString, message);
        // end temp identifier

        // loop over clients and send out the message; remove client if message failed
        // to be sent
        // Note: this uses a lambda expression for each item in the values() collection,
        // it's one way we can safely remove items during iteration
        
        connectedClients.values().removeIf(client -> {
            boolean failedToSend = !client.send(formattedMessage);
            if (failedToSend) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }

    /**
     * Attempts to see if the message is a command and process its action
     * 
     * @param message
     * @param sender
     * @return true if it was a command, false otherwise
     */
    private boolean processCommand(String message, ServerThread sender) {
        if(sender == null){
            return false;
        }
        System.out.println("Checking command: " + message);
        // disconnect
        if ("/disconnect".equalsIgnoreCase(message)) {
            ServerThread removedClient = connectedClients.get(sender.getClientId());
            if (removedClient != null) {
                disconnect(removedClient);
            }
            return true;
        }
        //vvh-10/07/24 - Example 2: Coin toss command (random heads or tails)
         if ("/flip".equalsIgnoreCase(message)) {//vvh-10/07/24 -checks if the command is /flip for coin flipping
        String result = Math.random() < 0.5 ? "heads" : "tails";//vvh-10/07/24 - decides randomly whether to return heads or tails for the coin flip
        String flipMessage = String.format("User[%s] flipped a coin and got %s", sender.getClientId(), result);//vvh-10/07/24 - formats the coin flip result message to include the user id and result 
        relay(flipMessage, null);//vvh-10/07/24 - broadcasts the coin flip result to all connected clients
        return true;//vvh-10/07/24 - exits the /flip command handling once the result is relayed
    }
        //vvh-10/07/24 - Example 6: Message shuffler (randomizes the order of the characters of the given message)
         if (message.startsWith("/shuffle ")) {//vvh-10/07/24 - checks if the command is /shuffle for message shuffling 
        String toShuffle = message.substring(9).trim(); //vvh-10/07/24 - extracts the message part that follows the /shuffle command 
        if (toShuffle.isEmpty()) {//vvh-10/07/24 - checks if the message to shuffle is empty 
            sender.send("please, enter a message along with shuffle.");//vvh-10/07/24 - sends an error message to the client if no message was provided to shuffle 
            return true;//vvh-10/07/24 - exits the /shuffle command handling if no message was provided 
        }
        String shuffledMessage = shuffleMessage(toShuffle);//vvh-10/07/24 - calls the shuffleMessage method to shuffle the provided message 
        String shuffleResult = String.format("User[%s] shuffled the message: %s", sender.getClientId(), shuffledMessage);//vvh-10/07/24 - formats the shuffled message with the user id 
        relay(shuffleResult, null); //vvh-10/07/24 - broadcasts the shuffled message to all connected clients 
        return true;//vvh-10/07/24 - exits the /shuffle command handling once the shuffled message is relayed 
    }
        // add more "else if" as needed
        return false;
    }

 // Helper method to shuffle a message

private String shuffleMessage(String message) {//vvh-10/07/24 -defines the shuffleMessage method that shuffles the characters of the message
    List<Character> characters = new ArrayList<>();//vvh-10/07/24 - creates a list to hold the characters of the message
    for (char c : message.toCharArray()) {//vvh-10/07/24 - loops over the characters in the message and adds them to the list 
        characters.add(c);//vvh-10/07/24 - adds each character to the list of characters 
    }
    Collections.shuffle(characters);//vvh-10/07/24 - shuffles the list of characters 
    StringBuilder shuffled = new StringBuilder();//vvh-10/07/24 - creates a StringBuilder to build the shuffled message 
    for (char c : characters) {//vvh-10/07/24 - loops over the shuffled list of characters 
        shuffled.append(c);//vvh-10/07/24 - appends each character to the StringBuilder to form the shuffled message
    }
    return shuffled.toString();//vvh-10/07/24 - returns the shuffled message as a string
}
    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}