package Project.Client.Views;

import java.awt.*;//vvh-12/09/24 importing awt classes 
import java.awt.event.ActionEvent;//vvh-12/09/24 importing actionevent for handling button actions 
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.*;//vvh-12/09/24 importing swing components 

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IQuestionEvents;//vvh-12/09/24 interface to handle questions events
import Project.Client.Interfaces.IRoomEvents;
import Project.Common.*;//vvh-12/09/24 importing all common classes 

public class GamePanel extends JPanel implements IQuestionEvents, IRoomEvents, IPhaseEvent {

    private JPanel playPanel;
    private CardLayout cardLayout;
    private static final String READY_PANEL = "READY";
    private static final String PLAY_PANEL = "PLAY";//example panel for this lesson
    public ReadyPanel readyPanel; //vvh-12/09/24 Panel shown when the game is in a "ready" phase
    JPanel buttonPanel;//vvh-12/09/24  Panel containing answer buttons for the game
    JLabel questionCategory;//vvh-12/09/24 Label to display the category of the current question
    JTextArea questionText = new JTextArea();//vvh-12/09/24 Text area to display the question text
    JButton answerButtonA = new JButton();
    JButton answerButtonB = new JButton();//vvh-12/09/24 Button for selecting answer option B
    JButton answerButtonC = new JButton();
    JButton answerButtonD = new JButton();
    JButton awayButton = new JButton("Mark Away");//vvh-12/09/24 Button to mark the user as away
    JButton spectateButton;//vvh-12/09/24 Button to toggle spectating mode


    public GamePanel(ICardControls controls) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setAlignmentY(Component.TOP_ALIGNMENT);

        readyPanel = new ReadyPanel();

        //vvh-12/09/24 Create the question category label
        questionCategory = new JLabel();
        questionCategory.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        questionText = new JTextArea();
        questionText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        questionText.setLineWrap(true);
        questionText.setEditable(false);
        questionText.setBackground(null);
        questionText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
    
        //vvh-12/09/24 Create the buttons and add them to a panel
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        //vvh-12/09/24 button A
        answerButtonA = new JButton("A");
        //vvh-12/09/24 Fill up the width of the button as parent
        answerButtonA.setSize(new Dimension(Integer.MAX_VALUE, answerButtonA.getHeight()));//Sets the button width to fill the maximum available space
        answerButtonA.addActionListener(event->{
            Client.INSTANCE.sendAnswer("A");//vvh-12/09/24 Sends "A" as the selected answer to the server
            onLockInAnswer(answerButtonA);
        });
        buttonPanel.add(answerButtonA);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        //vvh-12/09/24 button B
        answerButtonB = new JButton("B");
        answerButtonB.setSize(new Dimension(Integer.MAX_VALUE, answerButtonB.getHeight()));
        answerButtonB.addActionListener(event->{
            Client.INSTANCE.sendAnswer("B");//vvh-12/09/24 Sends "B" as the selected answer to the server
            onLockInAnswer(answerButtonB);

        });
        buttonPanel.add(answerButtonB);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        //vvh-12/09/24 button C
        answerButtonC = new JButton("C");
        answerButtonC.setSize(new Dimension(Integer.MAX_VALUE, answerButtonC.getHeight()));
        answerButtonC.addActionListener(event->{
            Client.INSTANCE.sendAnswer("C");//vvh-12/09/24 Sends "C" as the selected answer to the server
            onLockInAnswer(answerButtonC);

        });
        buttonPanel.add(answerButtonC);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        //vvh-12/09/24 button D
        answerButtonD = new JButton("D");
        answerButtonD.setSize(new Dimension(Integer.MAX_VALUE, answerButtonD.getHeight()));
        answerButtonD.addActionListener(event->{
            Client.INSTANCE.sendAnswer("D");//vvh-12/09/24 Sends "D" as the selected answer to the server
            onLockInAnswer(answerButtonD);

        });
        buttonPanel.add(answerButtonD);//vvh-12/09/24
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel gameContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) gameContainer.getLayout();
        this.setName(CardView.GAME_SCREEN.name());
        Client.INSTANCE.addCallback(this);

        readyPanel.setName(READY_PANEL);
        gameContainer.add(READY_PANEL, readyPanel);

        playPanel = new JPanel();
        playPanel.setLayout(new BoxLayout(playPanel, BoxLayout.Y_AXIS));//vvh-12/09/24 Creates the layout manager for the play panel to organize its components vertically
        playPanel.setAlignmentX(Component.LEFT_ALIGNMENT);//vvh-12/09/24 Aligns all components in the play panel to the left
        playPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        playPanel.setName(PLAY_PANEL);
        playPanel.add(questionCategory);//vvh-12/09/24 Adds the question category label to the play panel
        playPanel.add(questionText);//vvh-12/09/24 Adds the question text area to the play panel
        playPanel.add(buttonPanel);
//vvh-12/09/24   playPanel.add(Box.createVerticalGlue());
        awayButton.addActionListener(event->{//vvh-12/09/24 adds an action listener to the away button to handle clicks 
            onAwayButtonClicked(event);//vvh-12/09/24 calls the method to process the away button click event 
        });
        playPanel.add(awayButton);//vvh-12/09/24 Adds the "away" button to the play panel, allowing users to mark themselves as away
        playPanel.add(Box.createRigidArea(new Dimension(0, 5)));
//vvh-12/09/24 Button text changes based on the current spectating status
        spectateButton = new JButton(Client.INSTANCE.isSpectating() ? "Stop Spectating" : "Spectate");
        spectateButton.addActionListener(event->{
            if(Client.INSTANCE.isSpectating()) {//vvh-12/09/24 sends a request to start spectating
                Client.INSTANCE.sendSpectate(PayloadType.NOT_SPECTATE);
                spectateButton.setText("Spectate");//vvh-12/09/24 updates the button to spectate
            } else {
                Client.INSTANCE.sendSpectate(PayloadType.SPECTATE);
                spectateButton.setText("Stop Spectating");
                Client.INSTANCE.sendAnswer("AWAY");//vvh-12/09/24 sends away answer to the server
                answerButtonA.setEnabled(false);
                answerButtonB.setEnabled(false);//vvh-12/09/24 disables the answer button for option B while spectating 
                answerButtonC.setEnabled(false);
                answerButtonD.setEnabled(false);
            }
        });
        playPanel.add(spectateButton);

        gameContainer.add(PLAY_PANEL, playPanel);

        GameEventsPanel gameEventsPanel = new GameEventsPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameContainer, gameEventsPanel);
        splitPane.setResizeWeight(0.7);

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.7);
            }
        });

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playPanel.revalidate();
                playPanel.repaint();
            }
        });

        this.add(splitPane, BorderLayout.CENTER);
        controls.addPanel(CardView.CHAT_GAME_SCREEN.name(), this);
        setVisible(false);
    }

    private void onAwayButtonClicked(ActionEvent event) {//vvh-12/09/24 handles the action when the away button is clickled 
        String away = ((JButton) event.getSource()).getText();
        if (away.equals("Mark Away")) {
            Client.INSTANCE.sendAwayPayload(PayloadType.AWAY);
            ((JButton) event.getSource()).setText("Mark Not Away");
            Client.INSTANCE.sendAnswer("AWAY");
            answerButtonA.setEnabled(false);
            answerButtonB.setEnabled(false);
            answerButtonC.setEnabled(false);
            answerButtonD.setEnabled(false);
        } else {
            Client.INSTANCE.sendAwayPayload(PayloadType.NOT_AWAY);
            ((JButton) event.getSource()).setText("Mark Away");
        }
    }

    public void onLockInAnswer(JButton button) {//vvh-12/09/24 Disables all answer buttons and highlights the selected answer button
        answerButtonA.setEnabled(false);//vvh-12/09/24 Disable the first answer button (A) 
        answerButtonB.setEnabled(false);
        answerButtonC.setEnabled(false);
        answerButtonD.setEnabled(false);
        button.setBackground(Color.RED);//vvh-12/09/24 Highlight the selected answer button by changing its background color to red
    }

    @Override
    public void onRoomAction(long clientId, String clientName, String roomName, boolean isJoin) {
        if (Constants.LOBBY.equals(roomName) && isJoin) {
            setVisible(false);
            revalidate();
            repaint();
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {//vvh-12/09/24 Handles phase changes
        System.out.println("Received phase: " + phase.name());
        if (!isVisible()) {
            setVisible(true);
            getParent().revalidate();
            getParent().repaint();//vvh-12/09/24
            System.out.println("GamePanel visible");
        }
        if (phase == Phase.READY) {//vvh-12/09/24 Checks if the game phase is ready
            cardLayout.show(playPanel.getParent(), READY_PANEL);
            readyPanel.fetchCurrentCategory();
            buttonPanel.setVisible(false);
        } else if (phase == Phase.IN_PROGRESS) {//vvh-12/09/24 checks if the game is in-progress
            cardLayout.show(playPanel.getParent(), PLAY_PANEL);
            buttonPanel.setVisible(true);
        }
    }
    
    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // Not used here, but needs to be defined due to interface
    }

    @Override
    public void onAddQuestion() {//vvh-12/09/24 Opens a dialog for adding a new question
        AddQuestionDialog dialog = new AddQuestionDialog();
        dialog.setVisible(true);
    }

    @Override
    public void onQuestionReceived(String category, String question, List<String> answerOptions, String correctAnswer) {
        questionCategory.setText("Category: "+category);//vvh-12/09/24 Sets the category label to display the received category
        questionText.setText("Question: "+question);
        answerButtonA.setText(answerOptions.get(0));
        answerButtonB.setText(answerOptions.get(1));
        answerButtonC.setText(answerOptions.get(2));
        answerButtonD.setText(answerOptions.get(3));
//vvh-12/09/24 Resets the background of answer button 
        answerButtonA.setBackground(null);
        answerButtonB.setBackground(null);
        answerButtonC.setBackground(null);
        answerButtonD.setBackground(null);
//vvh-12/09/24
        if(!Client.INSTANCE.isAway() && !Client.INSTANCE.isSpectating()) {
            answerButtonA.setEnabled(true);
            answerButtonB.setEnabled(true);
            answerButtonC.setEnabled(true);
            answerButtonD.setEnabled(true);
        } else {//vvh-12/09/24
            answerButtonA.setEnabled(false);
            answerButtonB.setEnabled(false);
            answerButtonC.setEnabled(false);
            answerButtonD.setEnabled(false);
            Client.INSTANCE.sendAnswer("AWAY");
            if(Client.INSTANCE.isSpectating()) {
                LoggerUtil.INSTANCE.info("Spectating, answer is: "+correctAnswer);
                //vvh-12/09/24correct answer is shown
                if(correctAnswer.equals("A")) {//vvh-12/09/24 Compares the value of correctAnswer to the string "A"
                    answerButtonA.setBackground(Color.GREEN);
                } else if(correctAnswer.equals("B")) {
                    answerButtonB.setBackground(Color.GREEN);//vvh-12/09/24 Changes the background color of the corresponding button to green
                } else if(correctAnswer.equals("C")) {
                    answerButtonC.setBackground(Color.GREEN);
                } else if(correctAnswer.equals("D")) {
                    answerButtonD.setBackground(Color.GREEN);
                }
            }
        }
    }

}