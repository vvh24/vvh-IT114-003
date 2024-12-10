package Project.Client.Views;

import Project.Client.Client;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

/**
 * vvh - 12/09/24 This class allows users to input a question, category, answer options,
 * and specify the correct answer using radio buttons.
 */

public class AddQuestionDialog extends JDialog {
    private JTextField questionField;//vvh-12/09/24 input the question
    private JTextField categoryField;//vvh-12/09/24 input the category 
    private JTextField answerA;
    private JTextField answerB;//vvh-12/09/24 input for answer options 
    private JTextField answerC;
    private JTextField answerD;
    //vvh-12/09/24 radio buttons for correct answer
    private JRadioButton aButton;
    private JRadioButton bButton;
    private JRadioButton cButton;
    private JRadioButton dButton;
    //vvh-12/09/24 group for radio buttons
    private ButtonGroup buttonGroup;
    private JButton submitButton;
    private JButton cancelButton;

    public AddQuestionDialog() {//vvh-12/09/24 Sets up the dialog's layout, components, and functionality
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        this.setSize(600, 600);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setTitle("Add Question");//vvh - 12/09/24 set dialog title 
        //vvh-12/09/24 center the dialog
        this.setLocationRelativeTo(null);//vvh-12/09/24 
//vvh-12/09/24 add label and text field for the question 
        JLabel questionLabel = new JLabel("Question:");
        this.add(questionLabel);
        this.add(Box.createVerticalStrut(5));
        questionField = new JTextField();
        this.add(questionField);

        this.add(Box.createVerticalStrut(10));
//vvh-12/09/24 add label and text field for the category
        JLabel categoryLabel = new JLabel("Category:");
        this.add(categoryLabel);
        this.add(Box.createVerticalStrut(5));
        categoryField = new JTextField();
        this.add(categoryField);

        this.add(Box.createVerticalStrut(10));
//vvh-12/09/24 add labels and text fields for answer options
        JLabel answerALabel = new JLabel("Answer A:");
        this.add(answerALabel);
        this.add(Box.createVerticalStrut(5));
        answerA = new JTextField();
        this.add(answerA);

        this.add(Box.createVerticalStrut(10));

        JLabel answerBLabel = new JLabel("Answer B:");
        this.add(answerBLabel);
        this.add(Box.createVerticalStrut(5));
        answerB = new JTextField();
        this.add(answerB);

        this.add(Box.createVerticalStrut(10));

        JLabel answerCLabel = new JLabel("Answer C:");
        this.add(answerCLabel);
        this.add(Box.createVerticalStrut(5));
        answerC = new JTextField();
        this.add(answerC);

        this.add(Box.createVerticalStrut(10));

        JLabel answerDLabel = new JLabel("Answer D:");
        this.add(answerDLabel);
        this.add(Box.createVerticalStrut(5));
        answerD = new JTextField();
        this.add(answerD);

        this.add(Box.createVerticalStrut(10));

        JLabel correctAnswerLabel = new JLabel("Correct Answer:");//vvh-12/09/24 add label and radio buttons for selection the correct answer 
        this.add(correctAnswerLabel);

        this.add(Box.createVerticalStrut(5));
//vvh - 12/09/24 Initialize radio buttons
        aButton = new JRadioButton("A");
        aButton.setActionCommand("A");
        bButton = new JRadioButton("B");
        bButton.setActionCommand("B");
        cButton = new JRadioButton("C");
        cButton.setActionCommand("C");
        dButton = new JRadioButton("D");
        dButton.setActionCommand("D");
        buttonGroup = new ButtonGroup();
        buttonGroup.add(aButton);
        buttonGroup.add(bButton);
        buttonGroup.add(cButton);
        buttonGroup.add(dButton);
//vvh- 12/09/24 Add radio buttons to the dialog
        this.add(aButton);
        this.add(bButton);
        this.add(cButton);
        this.add(dButton);

        this.add(Box.createVerticalStrut(10));

        submitButton = new JButton("Submit");//vvh-12/09/24 Add Submit button and its action listener
        submitButton.addActionListener(l -> {
            if (!validateForm()) {
                return;
            }//vvh-12/09/24 Send question details to the server
            Client.INSTANCE.sendAddQuestion(questionField.getText(), categoryField.getText(), answerA.getText(), answerB.getText(), answerC.getText(), answerD.getText(), buttonGroup.getSelection().getActionCommand());
            this.dispose();
        });
        this.add(submitButton);

        this.add(Box.createVerticalStrut(10));

        cancelButton = new JButton("Cancel");//vvh-12/09/24 add cancel button 
        cancelButton.addActionListener(l -> {
            this.dispose();
        });
        this.add(cancelButton);
        this.add(Box.createVerticalStrut(10));

    }


    private boolean validateForm() {//vvh-12/09/24 Validates the form to ensure all fields are filled and a correct answer is selected
        if (questionField.getText().isEmpty() || categoryField.getText().isEmpty() || answerA.getText().isEmpty() || answerB.getText().isEmpty() || answerC.getText().isEmpty() || answerD.getText().isEmpty() || buttonGroup.getSelection() == null) {
            JOptionPane.showMessageDialog(this, "All fields are required", "Error", JOptionPane.ERROR_MESSAGE);
            return false;// vvh - 12/09/24 Form is invalid
        }
        return true;
    }

}
