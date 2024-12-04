package Project.Client.Views;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import javax.swing.*;

import Project.Client.Client;
import Project.Common.LoggerUtil;
import Project.Common.PayloadType;

public class ReadyPanel extends JPanel {
    JPanel questionsCategory = new JPanel();
    JLabel selectedCategory = new JLabel();
    JComboBox<String> categoryDropdown = new JComboBox<>();

    public ReadyPanel() {

        this.setLayout(new GridLayout(3, 1));

        questionsCategory.setLayout(new FlowLayout());

        selectedCategory.setText("Selected Category: All");
        questionsCategory.add(selectedCategory);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.ipadx = 10;
        gbc.ipady = 10;
        this.add(questionsCategory, gbc);
        JButton readyButton = new JButton();
        readyButton.setText("Ready");
        readyButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendReady();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);

        JButton addQuestionButton = new JButton();
        addQuestionButton.setText("Add Question");
        addQuestionButton.addActionListener(l -> {
            Client.INSTANCE.sendAddQuestion();
        });
        this.add(addQuestionButton);

////        boolean isSpec = Client.INSTANCE.isSpectating();
//        JButton spectate = new JButton();
//        spectate.setText("Spectate");
//        spectate.addActionListener(l -> {
//            Client.INSTANCE.sendSpectate(PayloadType.SPECTATE);
//        });
//        this.add(spectate);
    }

    public void fetchCurrentCategory() {
        Client.INSTANCE.fetchCurrentCategory();
    }

    public void categories(List<String> cats) {
        LoggerUtil.INSTANCE.info("Categories: " + cats);
        categoryDropdown.removeAllItems();
        categoryDropdown.addItem("All");

        for (String cat : cats) {
            categoryDropdown.addItem(cat);
        }
        questionsCategory.add(categoryDropdown);

        JButton chooseCategoriesButton = new JButton();
        chooseCategoriesButton.setText("Select Category");
        chooseCategoriesButton.addActionListener(l -> {
            Client.INSTANCE.sendCategory(categoryDropdown.getSelectedItem().toString());
        });
        questionsCategory.add(chooseCategoriesButton);
        questionsCategory.revalidate();
        questionsCategory.repaint();
    }

    public void categorySelected(String category) {
        selectedCategory.setText("Selected Category: " + category);
    }
}