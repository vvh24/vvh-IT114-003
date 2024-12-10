package Project.Client.Interfaces;

import java.util.List;

public interface IQuestionEvents extends IClientEvents {
    /**
     * vvh - 12/09/24 Called when a question is received by the client.
     *
     * @param category       The category to which the question belongs.
     * @param questionText   The text of the question being presented.
     * @param answerOptions  A list of possible answers for the question.
     * @param correctAnswer  The correct answer for the question.
     */
    void onQuestionReceived(String category, String questionText, List<String> answerOptions, String correctAnswer);

    void onAddQuestion();//vvh-12/09/24 Triggered when the client adds a new question
}
