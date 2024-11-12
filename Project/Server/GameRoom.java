package Project.Server;

import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.PointsPayload; // vvh - 11/10/24 Import PointsPayload for sending player scores
import Project.Common.QAPayload; // vvh - 11/10/24 Import for handling trivia questions as payloads.
import Project.Common.TimedEvent;
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
    
    private List<QAPayload> questions; // vvh - 11/10/24 List to store questions loaded from the file.
    private QAPayload currentQuestion; // vvh - 11/10/24 Current question for the round
    private Map<Long, Boolean> playerAnswers = new HashMap<>(); // vvh - 11/10/24 Track answers (clientId, isCorrect)

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerPlayer sp){
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);

        // vvh - 11/10/24 Sync current question and scores if game is in session
        if (currentPhase == Phase.IN_PROGRESS && currentQuestion != null) {
            syncCurrentQuestion(sp); //vvh - 11/11/24 Imports Collectors to aggregate player scores into a formatted scoreboard for display
            syncPlayerScores(sp); //vvh - 11/11/24 Synchronize current scores with the newly joined client
        }
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

    //vvh - 11/11/24 Determine if the answer is correct
        boolean isCorrect = answer.equalsIgnoreCase(currentQuestion.getCorrectAnswer());
        playerAnswers.put(clientId, isCorrect);
    // vvh -11/11/24 Notify all players that this player has locked in their answer
        String lockInMessage = player.getClientName() + " has locked in their answer.";
        playersInRoom.values().forEach(sp -> sp.sendMessage(lockInMessage)); // Broadcast to all players
    // vvh - 11/11/24 Log and update score if correct
        if (isCorrect) {
            LoggerUtil.INSTANCE.info("Player " + clientId + " answered correctly.");
            player.incrementScore(10); // vvh - 11/11/24 Awards 10 points to the player for a correct answer
        } else {
            LoggerUtil.INSTANCE.info("Player " + clientId + " answered incorrectly.");
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
                
                //vvh - 11/10/24 Parse each part to create a QAPayload.
                String questionText = parts[0];
                String category = parts[1];
                List<String> answerOptions = List.of(parts[2], parts[3], parts[4], parts[5]); //vvh - 11/10/24 Adjust this based on file format.
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
        if (questions == null || questions.isEmpty()) { //vvh - 11/10/24 Check if questions are loaded.
            LoggerUtil.INSTANCE.warning("No questions available to start the round."); //vvh - 11/10/24 Log warning if no questions loaded.
            onSessionEnd(); //vvh - 11/11/24  End the session if no questions are available
            return;
        }
        
        Random random = new Random(); //vvh - 11/10/24 Random object for selecting a question.
        int questionIndex = random.nextInt(questions.size()); // vvh - 11/10/24 Get a random index from the questions list.
        currentQuestion = questions.remove(questionIndex); //vvh - 11/11/24  Set and remove the chosen question
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
            null // vvh - 11/10/24 Send null as the correct answer to avoid revealing it to clients
        );

        //vvh - 11/10/24 Broadcast questionForClients to each connected client (implement actual client broadcast logic here)
        playersInRoom.values().forEach(sp -> sp.sendQuestion(questionForClients)); // vvh - 11/11/24 Broadcast question to each client
        LoggerUtil.INSTANCE.info("Question sent to clients: " + questionForClients); // vvh - 11/11/24 Log the broadcasted question without the answer.
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

    private void resetRoundTimer(){
        if(roundTimer != null){
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    private void awardPoints() { //vvh - 11/11/24 Method to award points to players with correct answers
        playerAnswers.forEach((clientId, isCorrect) -> {
            if (isCorrect) {
                ServerPlayer player = playersInRoom.get(clientId);
                if (player != null) {
                    player.incrementScore(10); // vvh - 11/11/24 Award points for correct answer
                    LoggerUtil.INSTANCE.info("Awarded 10 points to player " + player.getClientName() + ". Total score: " + player.getScore());
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

        if (currentRound >= MAX_ROUNDS || questions.isEmpty()) {
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
    }

    private void startTurnTimer(){
        turnTimer = new TimedEvent(30, ()-> onTurnEnd());
        turnTimer.setTickCallback((time)->System.out.println("Turn Time: " + time));
    }
    private void resetTurnTimer(){
        if(turnTimer != null){
            turnTimer.cancel();
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
        startRound();// vvh - 11/10/24 Start the first round by selecting and sending a question.
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart(){
        LoggerUtil.INSTANCE.info("onRoundStart() start");

        startRound(); // vvh - 11/10/24 Select and send a question to all clients at the start of the round.
        resetRoundTimer();
        startRoundTimer();

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
    protected void onSessionEnd(){
        endSession();
    }
    // end lifecycle methods

    

    // send/sync data to ServerPlayer(s)

    
    // end send data to ServerPlayer(s)

    // receive data from ServerThread (GameRoom specific)
    
    // end receive data from ServerThread (GameRoom specific)
}