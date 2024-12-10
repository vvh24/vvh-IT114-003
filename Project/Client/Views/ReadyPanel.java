package Project.Client.Views;

import java.awt.*;//vvh-12/09/24 importing awt classes
import java.io.IOException;
import java.util.List;//vvh-12/09/24 importing list to manage collection of categories 
import javax.swing.*;//vvh-12/09/24 importing swing components 

import Project.Client.Client;
import Project.Common.LoggerUtil;//vvh-12/09/24 importing for messages and errors 
import Project.Common.PayloadType;//vvh-12/09/24 importing to manage payload actions 

public class ReadyPanel extends JPanel {
    JPanel questionsCategory = new JPanel();//vvh-12/09/24 managing and displaying category-related elements 
    JLabel selectedCategory = new JLabel();//vvh-12/09/24 display currently selected category 
    JComboBox<String> categoryDropdown = new JComboBox<>();//vvh-12/09/24 dropdown menu for selecting a category

    public ReadyPanel() {

        this.setLayout(new GridLayout(3, 1));//vvh-12/09/24 set layout to grid with 3 rows and 1 column 

        questionsCategory.setLayout(new FlowLayout());//vvh-12/09/24 set layout of the questionscategory panel to flowlayout 

        selectedCategory.setText("Selected Category: All");//vvh-12/09/24 initializes the selectcategory label with a default value 
        questionsCategory.add(selectedCategory);
//vvh-12/09/24 configures gridbagconstraints object 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.ipadx = 10;
        gbc.ipady = 10;
        this.add(questionsCategory, gbc);//vvh-12/09/24 adds the questionscategory panel to the main panel 
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

        JButton addQuestionButton = new JButton();//vvh-12/09/24 creates a button for adding new questions
        addQuestionButton.setText("Add Question");
        addQuestionButton.addActionListener(l -> {
            Client.INSTANCE.sendAddQuestion();//vvh-12/09/24 sends a request to add a question to the server 
        });
        this.add(addQuestionButton);//vvh-12/09/24 adds the 'add question' button to the main panel 

////        boolean isSpec = Client.INSTANCE.isSpectating();
//        JButton spectate = new JButton();
//        spectate.setText("Spectate");
//        spectate.addActionListener(l -> {
//            Client.INSTANCE.sendSpectate(PayloadType.SPECTATE);
//        });
//        this.add(spectate);
    }

    public void fetchCurrentCategory() {//vvh-12/09/24 fetches the current category from the server 
        Client.INSTANCE.fetchCurrentCategory();
    }

    public void categories(List<String> cats) {//vvh-12/09/24 updates the category dropdown with the list of categories
        LoggerUtil.INSTANCE.info("Categories: " + cats);//vvh-12/09/24 Logs the list of categories
        categoryDropdown.removeAllItems();
        categoryDropdown.addItem("All");//vvh-12/09/24 adds 'all' as a default option to the dropdown 

        for (String cat : cats) {
            categoryDropdown.addItem(cat);//vvh-12/09/24 adds each category to the dropdown 
        }
        questionsCategory.add(categoryDropdown);

        JButton chooseCategoriesButton = new JButton();//vvh-12/09/24 Loops through the list of categories and adds each one to the dropdown
        chooseCategoriesButton.setText("Select Category");
        chooseCategoriesButton.addActionListener(l -> {//vvh-12/09/24 retrieves the currently selected item from the dropdown and sends it to the server using sendCategory
            Client.INSTANCE.sendCategory(categoryDropdown.getSelectedItem().toString());
        });//vvh-12/09/24 Adds the dropdown and button to the questionsCategory panel
        questionsCategory.add(chooseCategoriesButton);
        questionsCategory.revalidate();
        questionsCategory.repaint();
    }

    public void categorySelected(String category) {//vvh-12/09/24 updates the category label when a category is chosen 
        selectedCategory.setText("Selected Category: " + category);//vvh-12/09/24 sets the label text to display the selected category 
    }
}