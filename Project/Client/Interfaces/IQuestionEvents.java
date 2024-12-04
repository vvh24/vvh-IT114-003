package Project.Client.Interfaces;

import java.util.List;

public interface IQuestionEvents extends IClientEvents {
    void onQuestionReceived(String category, String questionText, List<String> answerOptions, String correctAnswer);

    void onAddQuestion();
}
