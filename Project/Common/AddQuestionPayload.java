package Project.Common;

public class AddQuestionPayload  extends Payload {
    private static final LoggerUtil logger = LoggerUtil.INSTANCE;
    private String questionText = "";
    private String category = "";
    private String answerA = "";
    private String answerB = "";
    private String answerC = "";
    private String answerD = "";
    private String correctAnswer = "";

    public boolean isQuestion() {
        return !getQuestionText().isBlank();
    }

    public AddQuestionPayload()
    {
        setPayloadType(PayloadType.ADD_QUESTION);
        setQuestionText("");
    }

    public AddQuestionPayload(String question, String category, String a, String b, String c, String d, String correct) {
        setPayloadType(PayloadType.ADD_QUESTION);
        this.questionText = question;
        this.category = category;
        this.answerA = a;
        this.answerB = b;
        this.answerC = c;
        this.answerD = d;
        this.correctAnswer = correct;
    }

    public void setQuestionText(String question)
    {
        this.questionText = question;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setCategory(String cateogry)
    {
        this.category = cateogry;
    }

    public String getCategory() {
        return category;
    }

    public void setAnswerA(String answer)
    {
        this.answerA = answer;
    }

    public String getAnswerA() {
        return answerA;
    }

    public void setAnswerB(String answer)
    {
        this.answerB = answer;
    }

    public String getAnswerB() {
        return answerB;
    }

    public void setAnswerC(String answer)
    {
        this.answerC = answer;
    }

    public String getAnswerC() {
        return answerC;
    }

    public void setAnswerD(String answer)
    {
        this.answerD = answer;
    }

    public String getAnswerD() {
        return answerD;
    }

    public void setCorrectAnswer(String answer)
    {
        this.correctAnswer = answer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

}
