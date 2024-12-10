package Project.Server;

import java.nio.file.Files;////vvh-12/09/24 Import for handling file operations, such as reading or writing files
import java.nio.file.Paths;//vvh-12/09/24 Import for working with file system paths
import java.nio.file.StandardOpenOption;//vvh-12/09/24 Import for specifying how files are opened or appended during write operations
import java.util.concurrent.ConcurrentHashMap;//vvh-12/09/24 Import for a thread-safe map implementation
import java.util.stream.Stream;//vvh-12/09/24 Import streams for processing collections 

import Project.Common.*;//vvh-12/09/24 import common classes 

import java.io.BufferedReader; // vvh - 11/10/24 Import for reading question data from a file.
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList; // vvh - 11/10/24 Import for storing questions in a list.
import java.util.List;
import java.util.Random; // vvh - 11/10/24 Import for randomly selecting a question.
import java.util.HashMap; //vvh - 11/10/24 Imports HashMap to store player answers for each round, associating client IDs with answer correctness
import java.util.Map; //vvh- 11/10/24 Imports Map to manage key-value pairs, such as client IDs and answer correctness
import java.util.Comparator;
import java.util.stream.Collectors; //vvh - 11/11/24 Imports Collectors to aggregate player scores into a formatted scoreboard for display


public class GameRoom extends BaseGameRoom {
    
    private static final int MAX_ROUNDS = 10; // vvh - 11/10/24 Define the number of rounds for a session
    private int currentRound = 0; // vvh - 11/10/24 Track the number of rounds completed

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;
    private int round = 0;
    
    private List<QAPayload> questions; // vvh - 11/10/24 List to store questions loaded from the file.
    private QAPayload currentQuestion; // vvh - 11/10/24 Current question for the round
    private Map<Long, Boolean> playerAnswers = new HashMap<>(); // vvh - 11/10/24 Track answers (clientId, isCorrect)
    private String category = "All";//vvh-12/09/24 Default category for questions, initially set to "All"
    private ConcurrentHashMap<ServerPlayer, Long> answerTimeMillis = new ConcurrentHashMap<>();//vvh-12/09/24


    public GameRoom(String name) {
        super(name);
    }

    private List<String> getQuestionCategories() {//vvh-12/09/24 Retrieves a list of unique question categories from the loaded questions
        if(questions == null) {//vvh-12/09/24 Checks if questions are not yet loaded, then loads them from the file
            loadQuestionsFromFile("Project/Resources/questions.txt");//vvh-12/09/24 Loads questions from the resources file path
        }//vvh-12/09/24 Extracts unique categories from the loaded questions
        return questions.stream().map(QAPayload::getCategory).distinct().collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerPlayer sp){
        // sync GameRoom state to new client

        // give a slight delay to allow the Room list content to be sent from the base
        // class
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                syncCurrentPhase(sp);
                syncReadyStatus(sp);
            }
        }.start();

    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerPlayer sp){
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + playersInRoom.size());
        if(playersInRoom.isEmpty()){
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    // vvh - 11/10/24 Method to process a player's answer
    public void processAnswer(long clientId, String answer) {
        ServerPlayer player = playersInRoom.get(clientId); //vvh -11/11/24 Retrieves the player based on clientId for answer processing
        if (player == null || playerAnswers.containsKey(clientId)) {
            LoggerUtil.INSTANCE.warning("Invalid answer submission by player " + clientId); //vvh - 11/11/24 Logs a warning if the player is invalid or has already answered
            return;
        }

        answerTimeMillis.put(player, System.currentTimeMillis());

    //vvh - 11/11/24 Determine if the answer is correct
        boolean isCorrect = answer.equalsIgnoreCase(currentQuestion.getCorrectAnswer());
        playerAnswers.put(clientId, isCorrect);

        if(!answer.equalsIgnoreCase("AWAY")) {//vvh-12/09/24 Processes the player's answer if it's not marked as "AWAY"
            // vvh -11/11/24 Notify all players that this player has locked in their answer
            String lockInMessage = player.getClientName() + " has locked in their answer ";
            // vvh - 11/11/24 Log and update score if correct
            if (isCorrect) {
                LoggerUtil.INSTANCE.info("Player " + clientId + " answered correctly.");
                lockInMessage += "correctly.";//vvh-12/09/24 Appends "correctly" to the lock-in message if the answer is correct
            } else {
                LoggerUtil.INSTANCE.info("Player " + clientId + " answered incorrectly.");
                lockInMessage += "incorrectly.";//vvh-12/09/24 Appends "incorrectly" to the lock-in message if the answer is wrong
            }
            String finalLockInMessage = lockInMessage;//vvh-12/09/24 Prepares the final lock-in message for broadcasting
            playersInRoom.values().forEach(sp -> sp.sendGameEvent(finalLockInMessage)); //vvh-12/09/24 Broadcast to all players
        }


    // vvh - 11/11/24 Check if all players have answered
        if (playerAnswers.size() == playersInRoom.size()) {
            LoggerUtil.INSTANCE.info("All players have answered. Ending round early.");
            onRoundEnd(); // vvh - 11/11/24 End the round early if all players have answered
        }
    }

    // vvh - 11/10/24 Method to sync current question to a specific client
    private void syncCurrentQuestion(ServerPlayer sp) {
        QAPayload questionForClient = new QAPayload(
            currentQuestion.getClientId(), //vvh - 11/11/24 Sends the client ID of the current question to the client
            currentQuestion.getQuestionText(),
            currentQuestion.getCategory(),
            currentQuestion.getAnswerOptions(),
            null //vvh - 11/11/24  Send null as correct answer to avoid revealing it
        );
        sp.sendQuestion(questionForClient);
        LoggerUtil.INSTANCE.info("Synced current question to player: " + sp.getClientId());
    }

    // vvh - 11/10/24 Method to sync current player scores to a specific client
    private void syncPlayerScores(ServerPlayer sp) {
        String scoreboard = generateScoreboard();
        sp.sendMessage(scoreboard);
        LoggerUtil.INSTANCE.info("Synced player scores to player: " + sp.getClientId());
    }

    // vvh - 11/10/24 Method to load questions from a file and store them in memory.
    private void loadQuestionsFromFile(String filePath) {
        questions = new ArrayList<>(); //vvh - 11/10/24 Initialize the list to store QAPayload objects.
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) { // vvh - 11/11/24 BufferedReader for reading file line-by-line.
            String line;
            while ((line = br.readLine()) != null) { //vvh - 11/10/24 Read each line of the file.
                String[] parts = line.split(","); //vvh - 11/10/24 Split line by commas (or any chosen delimiter).
                 if (parts.length < 7) { 
                    LoggerUtil.INSTANCE.warning("Invalid question format: " + line);
                    continue; // Skip the invalid line
                }
                //vvh - 11/10/24 Parse each part to create a QAPayload.
                String questionText = parts[0];
                String category = parts[1];
                List<String> answerOptions = List.of(parts[2], parts[3], parts[4], parts[5]); //vvh - 11/10/24 
                String correctAnswer = parts[6];
                
                //vvh - 11/10/24 Create a new QAPayload object with parsed data.
                QAPayload question = new QAPayload(0, questionText, category, answerOptions, correctAnswer);
                
                questions.add(question); //vvh - 11/10/24 Add the question to the list.
            }
            LoggerUtil.INSTANCE.info("Questions loaded successfully."); //vvh - 11/10/24 Log success message.
        } catch (IOException e) { //vvh - 11/10/24 Handle file exceptions.
            LoggerUtil.INSTANCE.severe("Error reading questions file: " + e.getMessage()); //vvh - 11/10/24 Log error message.
        }
    }
    // vvh - 11/10/24 Method to start the first round by selecting and sending a question.
    private void startRound() {
        if(category == null || category == "") {//vvh-12/09/24  Sets the default category to "All" if none is specified
            category = "All";//vvh-12/09/24
        }

        if (category.equalsIgnoreCase("All")) {//vvh-12/09/24 Processes all questions if the category is "All"
            if (questions == null || questions.isEmpty()) { //vvh - 11/10/24 Check if questions are loaded.
                LoggerUtil.INSTANCE.warning("No questions available to start the round."); //vvh - 11/10/24 Log warning if no questions loaded.
                onSessionEnd(); //vvh - 11/11/24  End the session if no questions are available
                return;
            }
        } else {//vvh-12/09/24 Filters questions based on the selected category
            List<QAPayload> categoryQuestions = questions.stream().filter(q -> q.getCategory().equalsIgnoreCase(category)).collect(Collectors.toList());
            if (categoryQuestions.isEmpty()) {
                LoggerUtil.INSTANCE.warning("No questions available for the selected category to start the round."); //vvh - 11/10/24 Log warning if no questions are available for the selected category.
                onSessionEnd(); //vvh - 11/11/24  End the session if no questions are available for the selected category
                return;
            }
        }
        
        Random random = new Random(); //vvh - 11/10/24 Random object for selecting a question.
        List<QAPayload> categoryQuestions = category.isEmpty() || category.equalsIgnoreCase("All") ? questions : questions.stream().filter(q -> q.getCategory().equalsIgnoreCase(category)).collect(Collectors.toList());
        int questionIndex = random.nextInt(categoryQuestions.size()); // vvh - 11/10/24 Get a random index from the questions list.
        currentQuestion = categoryQuestions.remove(questionIndex); //vvh - 11/11/24  Set and remove the chosen question
        questions.remove(currentQuestion); //vvh - 11/11/24 Remove the chosen question from the list
        playerAnswers.clear(); // vvh - 11/11/24 Clear previous round answers
        sendQuestionToClients(currentQuestion);

        LoggerUtil.INSTANCE.info("Round started with question: " +
                             currentQuestion.getQuestionText() + 
                             ", Category: " + currentQuestion.getCategory() + 
                             ", Options: " + currentQuestion.getAnswerOptions());

        resetRoundTimer();
        startRoundTimer();
    }

    // vvh - 11/10/24 Method to send a question to all clients without revealing the correct answer.
    private void sendQuestionToClients(QAPayload question) {
        QAPayload questionForClients = new QAPayload(
            question.getClientId(),
            question.getQuestionText(),
            question.getCategory(),
            question.getAnswerOptions(),
            question.getCorrectAnswer() // vvh - 11/10/24 Send null as the correct answer to avoid revealing it to clients
        );

        //vvh - 11/10/24 Broadcast questionForClients to each connected client (implement actual client broadcast logic here)
        playersInRoom.values().forEach(sp -> sp.sendQuestion(questionForClients)); // vvh - 11/11/24 Broadcast question to each client
        LoggerUtil.INSTANCE.info("Question sent to clients: " + questionForClients + " - "+question.getCorrectAnswer()); // vvh - 11/11/24 Log the broadcasted question without the answer.
    }

    // timer handlers
    private void startRoundTimer(){
        roundTimer = new TimedEvent(30, this::onRoundEnd); //vvh- 11/11/24 Initialize with 30 seconds
        roundTimer.setTickCallback((time) -> {
            //vvh - 11/11/24 Log the countdown on the server only, without broadcasting to clients
            LoggerUtil.INSTANCE.info("Time remaining on server: " + time + " seconds");

            // vvh - 11/11/24 Send every second (like a stopwatch)
            if (time <= 5){
            String timeMessage = "Time remaining: " + time + " seconds";
            playersInRoom.values().forEach(sp -> sp.sendMessage(timeMessage));
            }
        });
    //roundTimer.start(); // vvh - 11/11/24 Starts the timer event
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }

    private void awardPoints() { //vvh - 11/11/24 Method to award points to players with correct answers
        playerAnswers.forEach((clientId, isCorrect) -> {
            if (isCorrect) {
                ServerPlayer player = playersInRoom.get(clientId);//vvh-12/09/24 Retrieves the player's details for point calculation
                long millis = answerTimeMillis.get(player);//vvh-12/09/24 Fetches the response time for the player
                long fastest = answerTimeMillis.values().stream().min(Comparator.naturalOrder()).get();//vvh-12/09/24 Identifies the fastest response time among all players

                if (player != null) {
                    //vvh-12/09/24 Max increment = 10, increase based on least time taken after first
                    int points = 10 - (int) ((millis - fastest) / 1000);
                    player.incrementScore(points);//vvh-12/09/24  Updates the player's score
                    LoggerUtil.INSTANCE.info("Awarded points to player " + player.getClientName() + ". Total score: " + player.getScore());
                }
            }
        });
    }

    private void syncScoresWithClients() { //vvh 11/11/24 method broadcasts the generated scoreboard to all players
        LoggerUtil.INSTANCE.info("Syncing scores with clients: " + generateScoreboard());
        String scoreboard = generateScoreboard();
        playersInRoom.values().forEach(sp -> sp.sendMessage(scoreboard));
    }

    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");

        awardPoints(); //vvh - 11/11/24 Awards points to players based on answer correctness
        syncScoresWithClients(); //vvh - 11/11/24 Updates all clients with the latest scoreboard
        playerAnswers.clear();
        currentRound++; //vvh - 11/10/24 Increment round counter
        List<QAPayload> categoryQuestions = category.isEmpty() || category.equalsIgnoreCase("All") ? questions : questions.stream().filter(q -> q.getCategory().equalsIgnoreCase(category)).collect(Collectors.toList());//vvh-12/09/24
        if (currentRound >= MAX_ROUNDS || questions.isEmpty() || categoryQuestions.isEmpty()) {
            endSession();  // End the session directly
        } else {
            onRoundStart();
        }

        LoggerUtil.INSTANCE.info("onRoundEnd() end");
    }

    private String generateScoreboard() {
        return playersInRoom.values().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore())) // vvh - 11/11/24 Sort by score descending
            .map(p -> p.getClientName() + ": " + p.getScore() + " points") // vvh - 11/11/24 Format each player's score
            .collect(Collectors.joining("\n"));
    }

    private void endSession() { //vvh 11/11/24 Sends the final scoreboard to all players and thanks them for playing.
        String finalScoreboard = "Game Over! Final Scoreboard:\n" + generateScoreboard() +
                                 "\nThank you for playing! A new session can begin after everyone is ready.\n";
        playersInRoom.values().forEach(sp -> sp.sendMessage(finalScoreboard));

        resetGameData();
        resetReadyStatus();//vvh 11/11/24 Resets player data, clears scores, and prepares for a potential new session.
        changePhase(Phase.READY);

        LoggerUtil.INSTANCE.info("Session has ended and players are now back in the READY phase.");
    }

    private void resetGameData() { //vvh -11/11/24 Method to reset game data at the end of a session
        playersInRoom.values().forEach(sp -> sp.resetScore()); // vvh - 11/11/24 Resets all players’ scores
        playerAnswers.clear();
        currentRound = 0;
        resetRoundTimer();//vvh-12/09/24 Resets the round timer when the round ends
        resetTurnTimer();

    }

    private void startTurnTimer(){
        turnTimer = new TimedEvent(30, ()-> onTurnEnd());//Initialize the Timer
        turnTimer.setTickCallback((time)->System.out.println("Turn Time: " + time));//Sets a callback function to be executed at regular intervals 
    }
    private void resetTurnTimer(){//Cancels the active timer, ensuring that it doesn't trigger the end-of-turn logic
        if(turnTimer != null){
            turnTimer.cancel();//Calls the cancel method on the TimedEvent object, which stops the timer from running further
            turnTimer = null;
        }
    }
    // end timer handlers
    
    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart(){
        LoggerUtil.INSTANCE.info("onSessionStart() start");

        loadQuestionsFromFile("Project/Resources/questions.txt"); // vvh - 11/10/24 Load questions at the beginning of the session.
        changePhase(Phase.IN_PROGRESS);

        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        round++;
        sendGameEvent("Round: " + round);
        startRound();
        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart(){
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd(){
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring
        
        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        resetReadyStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }
    // end lifecycle methods

    // send/sync data to ServerPlayer(s)
    /**
     * Sends the turn status of one Player to all Players (including themselves)
     * 
     * @param sp
     */
    private void sendTurnStatus(ServerPlayer sp) {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(sp.getClientId(), sp.didTakeTurn());
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    /**
     * Sends a game event to all clients
     * @param str
     */
    private void sendGameEvent(String str) {
        sendGameEvent(str, null);
    }

    /**
     * Sends a game event to specific clients (by id)
     * @param str
     * @param targets
     */
    private void sendGameEvent(String str, List<Long> targets) {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean canSend = false;
            if (targets != null) {
                if (targets.contains(spInRoom.getClientId())) {
                    canSend = true;
                }
            } else {
                canSend = true;
            }
            if (canSend) {
                boolean failedToSend = !spInRoom.sendGameEvent(str);
                if (failedToSend) {
                    removedClient(spInRoom.getServerThread());
                }
                return failedToSend;
            }
            return false;
        });
    }

    private void sendPointsUpdate(ServerPlayer sp) {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPointsUpdate(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }
    // end send data to ServerPlayer(s)

    // start custom checks
    private void checkPlayerIsReady(ServerPlayer sp) throws Exception {
        if (!sp.isReady()) {
            sp.sendGameEvent("You weren't ready in time");
            throw new Exception("Player isn't ready");
        }
    }

    private void checkPlayerTookTurn(ServerPlayer sp) throws Exception {
        if (sp.didTakeTurn()) {
            sp.sendGameEvent("You already took your turn");
            throw new Exception("Player already took turn");
        }
    }
    // end custom checks

    // receive data from ServerThread (GameRoom specific)
    protected void handleTurn(ServerThread sender) {
        try {
            // early exit checks
            checkPlayerInRoom(sender);
            checkCurrentPhase(sender, Phase.IN_PROGRESS);

            ServerPlayer sp = playersInRoom.get(sender.getClientId());
            checkPlayerIsReady(sp);
            checkPlayerTookTurn(sp);

            sp.setTakeTurn(true);
            sendTurnStatus(sp);
            // example of fastest to take a turn
            long ready = playersInRoom.values().stream().filter(p -> p.isReady()).count() + 1;
            long tookTurn = playersInRoom.values().stream().filter(p -> p.isReady() && p.didTakeTurn()).count();
            int points = (int) (ready - tookTurn);
            sp.changePoints(points);
            sendPointsUpdate(sp);
            if (didAllTakeTurn()) {
                onRoundEnd();
            }

        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleReady exception", e);
        }
    }

    // end receive data from ServerThread (GameRoom specific)
    // misc logic
    private boolean didAllTakeTurn() {
        long ready = playersInRoom.values().stream().filter(p -> p.isReady()).count();
        long tookTurn = playersInRoom.values().stream().filter(p -> p.isReady() && p.didTakeTurn()).count();
        LoggerUtil.INSTANCE.info(String.format("didAllTakeTurn() %s/%s", tookTurn, ready));
        return ready == tookTurn;
    }
//vvh-12/09/24
    public void addQuestion(long clientId, Payload payload) {
        AddQuestionPayload addQuestionPayload = (AddQuestionPayload) payload;
        saveQuestionToFile(addQuestionPayload);//vvh-12/09/24 Saves the new question to the file.
    }
//vvh-12/09/24
    private void saveQuestionToFile(Payload payload) {
        AddQuestionPayload addQuestionPayload = (AddQuestionPayload) payload;
        long clientId = addQuestionPayload.getClientId();
        ServerPlayer player = playersInRoom.get(clientId);

        if (player == null) { // Validate if the client exists
            LoggerUtil.INSTANCE.warning("Invalid clientId: " + clientId + ". Unable to save question.");
            return; // Stop further processing
        }
        String question = addQuestionPayload.getQuestionText();
        String category = addQuestionPayload.getCategory();
        String answerA = addQuestionPayload.getAnswerA();//vvh-12/09/24 Retrieves the text for answer A
        String answerB = addQuestionPayload.getAnswerB();
        String answerC = addQuestionPayload.getAnswerC();
        String answerD = addQuestionPayload.getAnswerD();//vvh-12/09/24 Retrieves the text for answer D
        String correctAnswer = addQuestionPayload.getCorrectAnswer();

        if (question == null || category == null || correctAnswer == null || answerA == null || answerB == null || answerC == null || answerD == null) {
        LoggerUtil.INSTANCE.warning("Invalid question payload from client: " + clientId);
        return;
        }
//vvh-12/09/24
        String questionData = String.format("\n%s,%s,%s,%s,%s,%s,%s", question, category, answerA, answerB, answerC, answerD, correctAnswer);

        try {//vvh-12/09/24 Attempts to append the new question to the file
            Files.write(Paths.get("Project/Resources/questions.txt"), questionData.getBytes(), StandardOpenOption.APPEND);
            LoggerUtil.INSTANCE.info("Question added to file: " + questionData);
            sendGameEvent(String.format("%s [%s] added a question in the questions bank.", playersInRoom.get(addQuestionPayload.getClientId()).getClientName(), addQuestionPayload.getClientId()));
        } catch (IOException e) {//vvh-12/09/24 handle errors during file write operations 
            LoggerUtil.INSTANCE.severe("Error adding question to file: " + e.getMessage());

        }
    }

    public void handleAway(long clientId, boolean isAway) {//vvh-12/09/24 Updates the player's "away" status
        ServerPlayer sp = playersInRoom.get(clientId);
        if (sp != null) {
            sp.setAway(isAway);//vvh-12/09/24 
            sendGameEvent(String.format("%s [%s] is now %s", sp.getClientName(), sp.getClientId(), isAway ? "away" : "no longer away"));
            playersInRoom.values().forEach(p -> {//vvh-12/09/24 Notifies all players about the "away" status change
                p.sendAwayStatus(sp.getClientId(), isAway);
            });
        }
    }
//vvh-12/09/24
    public void spectate(long clientId, Payload payload) {
        ServerPlayer sp = playersInRoom.get(clientId);
        if (sp != null) {//vvh-12/09/24
            boolean isSpectating = payload.getPayloadType() == PayloadType.SPECTATE;
            sp.setSpectating(isSpectating);//vvh-12/09/24 Updates the player's spectating status
            sendGameEvent(String.format("%s [%s] is now %s", sp.getClientName(), sp.getClientId(), isSpectating ? "spectating" : "no longer spectating"));
            playersInRoom.values().forEach(p -> { // vvh-12/09/24 // Notifies all players about the spectating status change
                p.sendSpectateStatus(sp.getClientId(), isSpectating);
            });
        }
    }
//vvh-12/09/24
    public void sendCategories(long clientId) {//vvh-12/09/24
        ServerPlayer sp = playersInRoom.get(clientId);
        if (sp != null) {//vvh-12/09/24
            List<String> categories = getQuestionCategories();
            sp.sendCategories(categories);
        }
    }
//vvh-12/09/24
    public void setCategory(String selectedCategory) {
        category = selectedCategory;//vvh-12/09/24 Updates the selected category
        sendGameEvent("Category set to: " + selectedCategory);//vvh-12/09/24 Notifies all players about the updated category

        playersInRoom.values().forEach(sp -> {//vvh-12/09/24 Sends the selected category to all players
            sp.sendCategory(selectedCategory);
        });
    }

    public String getCategory() {//vvh-12/09/24
        return category;
    }

    public void sendCategory(long clientId, String currentCategory) {//vvh-12/09/24
        ServerPlayer sp = playersInRoom.get(clientId);
        if (sp != null) {//vvh-12/09/24
            sp.sendCategory(currentCategory);
        }
    }
    // end misc logic
}