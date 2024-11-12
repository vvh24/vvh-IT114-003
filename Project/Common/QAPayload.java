package Project.Common;

import java.util.List; // vvh - 11/10/24 Importing List to store multiple answer options.
import Project.Common.LoggerUtil; // vvh - 11/10/24 Importing LoggerUtil for structured logging.

// vvh - 11/10/24 QAPayload class, a specialized type of Payload for handling trivia questions and answer options.
public class QAPayload extends Payload {
    private static final LoggerUtil logger = LoggerUtil.INSTANCE; // vvh - 11/10/24 LoggerUtil instance for logging debug information.

    // vvh - 11/10/24 Fields specific to QAPayload
    private String questionText; // vvh - 11/10/24 The text of the trivia question.
    private String category; // vvh - 11/10/24 The category of the trivia question.
    private List<String> answerOptions; // vvh - 11/10/24 A list of answer options for the trivia question.
    private String correctAnswer; // vvh - 11/10/24 The correct answer indicator (e.g., "A", "B", "C", "D").

    // vvh - 11/10/24 Constructor for QAPayload, initializing clientId, payloadType, message, and specific fields for questions.
    public QAPayload(long clientId, String questionText, String category, List<String> answerOptions, String correctAnswer) {
        setClientId(clientId); // vvh - 11/10/24 Set the client ID for this payload.
        setPayloadType(PayloadType.QUESTION); // vvh - 11/10/24 Set the payload type to QUESTION.
        setMessage("Question Payload"); // vvh - 11/10/24 Set a message indicating this is a question payload.
        this.questionText = questionText; // vvh - 11/10/24 Assign the question text to this instance.
        this.category = category; // vvh - 11/10/24 Assign the category of the question to this instance.
        this.answerOptions = answerOptions; // vvh - 11/10/24 Assign the list of answer options to this instance.
        this.correctAnswer = correctAnswer; // vvh - 11/10/24 Assign the correct answer indicator to this instance.
    }

    // vvh - 11/10/24 Getter for questionText field, returns the question text.
    public String getQuestionText() { 
        return questionText; 
    }

    // vvh - 11/10/24 Getter for category field, returns the question category.
    public String getCategory() { 
        return category; 
    }

    // vvh - 11/10/24 Getter for answerOptions field, returns the list of answer options.
    public List<String> getAnswerOptions() { 
        return answerOptions; 
    }

    // vvh - 11/10/24 Getter for correctAnswer field, returns the correct answer indicator.
    public String getCorrectAnswer() { 
        return correctAnswer; 
    }

    // vvh - 11/10/24 Override toString with logging for debug output
    @Override
    public String toString() {
        // vvh - 11/10/24 Format the question payload details, including payload type, client ID, question text, category, options, and correct answer.
        String debugInfo = String.format(
            "QAPayload[Type: %s, Client ID: %s, Question: %s, Category: %s, Options: %s, Correct Answer: %s]", 
            getPayloadType(), getClientId(), questionText, category, answerOptions, correctAnswer
        );
        
        // vvh - 11/10/24 Log the formatted details of the question payload for debugging purposes.
        logger.info(debugInfo); // vvh - 11/10/24 Logs the question payload details at the INFO level.
        
        return debugInfo; // vvh - 11/10/24 Returns the formatted string for external use if needed.
    }
}
