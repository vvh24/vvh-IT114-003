package Project.Client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;//vvh-12/09/24 adding events classes
import java.io.IOException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import Project.Client.Interfaces.*;//vvh-12/09/24 adding all interfaces files 
import Project.Client.Views.ChatGamePanel;
import Project.Client.Views.ConnectionPanel;
import Project.Client.Views.Menu;
import Project.Client.Views.RoomsPanel;
import Project.Client.Views.UserDetailsPanel;

import Project.Common.LoggerUtil;

/**
 * ClientUI is the main application window that manages different screens and
 * handles client events.
 */
public class ClientUI extends JFrame implements ICategoryEvents, ISpectateEvents, IAwayStatus, IConnectionEvents, IMessageEvents, IRoomEvents, ICardControls {
    private CardLayout card = new CardLayout(); // Layout manager to switch between different screens
    private Container container; // Container to hold different panels
    private JPanel cardContainer;
    private String originalTitle;
    private JPanel currentCardPanel;
    private CardView currentCard = CardView.CONNECT;
    private JMenuBar menu;
    private ConnectionPanel connectionPanel;
    private UserDetailsPanel userDetailsPanel;
    private ChatGamePanel chatGamePanel;
    private RoomsPanel roomsPanel;
    private JLabel roomLabel = new JLabel();

    private boolean isSortedUserList = false;//vvh-12/09/24 tracks whether the user list is currently sorted 

    {
        // Note: Moved from Client as this file is the entry point now
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }

    /**
     * Constructor to create the main application window.
     * 
     * @param title The title of the window.
     */
    public ClientUI(String title) {
        super(title); // Call the parent's constructor to set the frame title
        originalTitle = title;
        container = getContentPane();
        cardContainer = new JPanel();
        cardContainer.setLayout(card);
        container.add(roomLabel, BorderLayout.NORTH);
        container.add(cardContainer, BorderLayout.CENTER);

        cardContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                cardContainer.setPreferredSize(e.getComponent().getSize());
                cardContainer.revalidate();
                cardContainer.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // No specific action on move
            }
        });

        setMinimumSize(new Dimension(600, 600));//vvh-12/09/24 changed size dimension 
        setLocationRelativeTo(null); // Center the window
        menu = new Menu(this);
        this.setJMenuBar(menu);

        // Initialize panels
        connectionPanel = new ConnectionPanel(this);
        userDetailsPanel = new UserDetailsPanel(this);
        chatGamePanel = new ChatGamePanel(this);
        roomsPanel = new RoomsPanel(this);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                int response = JOptionPane.showConfirmDialog(cardContainer,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) {
                    try {
                        Client.INSTANCE.sendDisconnect();
                    } catch (NullPointerException | IOException e) {
                        LoggerUtil.INSTANCE.severe("Error during disconnect: " + e.getMessage());
                    }
                    System.exit(0);
                }
            }
        });
        
        this.addHierarchyListener(new HierarchyListener() {//vvh-12/09/24 adds a listener to detect chanages in the component hierarchy 

            @Override
            public void hierarchyChanged(HierarchyEvent e) {//vvh-12/09/24 handles changes in the component hierarchy and sets up listeners if connected 
                boolean connected = setupListenersWhenConnected();

                if (connected) {//vvh-12/09/24 Removes the hierarchy listener once the setup is complete and the connection is established
                    ClientUI.this.removeHierarchyListener(this);
                }
            }

            private boolean setupListenersWhenConnected() {//vvh-12/09/24 Checks if the window's parent frame is connected and adds appropriate listeners
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(ClientUI.this);
                if (parentFrame == null) {
                    return false;
                }
                parentFrame.addWindowListener(new WindowAdapter() {//vvh-12/09/24 Adds a window listener to handle the closing event of the parent frame

                    @Override
                    public void windowClosing(WindowEvent e) {//vvh-12/09/24 Handles the closing of the parent frame and sends a disconnect request
                        try {
                            Client.INSTANCE.sendDisconnect();//vvh-12/09/24 Sends a disconnect payload when the window is closing
                        } catch (NullPointerException | IOException ex) {
                            LoggerUtil.INSTANCE.severe("Error during disconnect: " + ex.getMessage());
                        }
                    }
                });
                return true;
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {//vvh-12/09/24 Adds a shutdown hook to perform cleanup tasks when the JVM shuts down
            LoggerUtil.INSTANCE.info("JVM is shutting down. Perform cleanup tasks.");
            try {
                Client.INSTANCE.sendDisconnect();//vvh-12/09/24 Sends a disconnect payload during the JVM shutdown process
            } catch (IOException e) {//vvh-12/09/24 Handles errors if sending the disconnect payload fails
                LoggerUtil.INSTANCE.severe("Error during disconnect: " + e.getMessage());
            }
        }));

        pack(); // Resize to fit components
        setVisible(true); // Show the window

//vvh-12/09/24  //After each 3 seconds, sort the user list
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(1000);
//                    chatGamePanel.getChatPanel().sortUserList();
//
//                } catch (InterruptedException e) {
//                    LoggerUtil.INSTANCE.severe("Error during user list sorting: " + e.getMessage());
//                }
//            }
//        }).start();
    }

    /**
     * Finds the current visible panel and updates the current card state.
     */
    private void findAndSetCurrentPanel() {
        for (Component c : cardContainer.getComponents()) {
            if (c.isVisible()) {
                currentCardPanel = (JPanel) c;
                currentCard = Enum.valueOf(CardView.class, currentCardPanel.getName());
                // Ensure connection for specific views
                if (Client.INSTANCE.getMyClientId() == ClientPlayer.DEFAULT_CLIENT_ID
                        && currentCard.ordinal() >= CardView.CHAT.ordinal()) {
                    show(CardView.CONNECT.name());
                }
                break;
            }
        }
        LoggerUtil.INSTANCE.fine("Current panel: " + currentCardPanel.getName());
    }

    @Override
    public void next() {
        card.next(cardContainer);
        findAndSetCurrentPanel();
    }

    @Override
    public void previous() {
        card.previous(cardContainer);
        findAndSetCurrentPanel();
    }

    @Override
    public void show(String cardName) {
        card.show(cardContainer, cardName);
        findAndSetCurrentPanel();
    }

    @Override
    public void addPanel(String cardName, JPanel panel) {
        cardContainer.add(panel, cardName);
    }

    @Override
    public void connect() {
        String username = userDetailsPanel.getUsername();
        String host = connectionPanel.getHost();
        int port = connectionPanel.getPort();
        setTitle(originalTitle + " - " + username);
        Client.INSTANCE.connect(host, port, username, this);
    }

    public static void main(String[] args) {
        // TODO update with your UCID instead of mine
        SwingUtilities.invokeLater(() -> new ClientUI("MT85-Client"));
    }
    // Interface methods start

    @Override
    public void onClientDisconnect(long clientId, String clientName) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatGamePanel.getChatPanel().removeUserListItem(clientId);
            boolean isMe = clientId == Client.INSTANCE.getMyClientId();
            String message = String.format("*%s disconnected*",
                    isMe ? "You" : String.format("%s[%s]", clientName, clientId));
                    chatGamePanel.getChatPanel().addText(message);
            if (isMe) {
                LoggerUtil.INSTANCE.info("I disconnected");
                previous();
            }
        }
    }

    @Override
    public void onMessageReceive(long clientId, String message) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            if (clientId < ClientPlayer.DEFAULT_CLIENT_ID) {
                // Note: Planning to use < -1 as internal channels (see GameEventsPanel)
                return;
            }
            String clientName = clientId > 0 ? Client.INSTANCE.getClientNameFromId(clientId) : "SERVER";
            chatGamePanel.getChatPanel().addText(
                    clientId > 0 ? String.format("%s[%s]: %s", clientName, clientId, message)
                            :
                    String.format("%s: %s", clientName, message)
            );
        }
    }

    @Override
    public void onReceiveClientId(long id) {
        show(CardView.CHAT_GAME_SCREEN.name());
        chatGamePanel.getChatPanel().addText("*You connected*");
    }

    @Override
    public void onResetUserList() {
        chatGamePanel.getChatPanel().clearUserList();
    }

    @Override
    public void onSyncClient(long clientId, String clientName) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatGamePanel.getChatPanel().addUserListItem(clientId, String.format("%s (%s)", clientName, clientId));
        }
        isSortedUserList = false;//vvh-12/09/24 Resets the user list sorting state when synchronizing a client
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        roomsPanel.removeAllRooms();
        if (message != null && !message.isEmpty()) {
            roomsPanel.setMessage(message);
        }
        if (rooms != null) {
            for (String room : rooms) {
                roomsPanel.addRoom(room);
            }
        }
    }

    @Override
    public void onRoomAction(long clientId, String clientName, String roomName, boolean isJoin) {
        LoggerUtil.INSTANCE.info("Current card: " + currentCard.name());
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            boolean isMe = clientId == Client.INSTANCE.getMyClientId();
            String message = String.format("*%s %s the Room %s*",
                    /* 1st %s */ isMe ? "You" : String.format("%s[%s]", clientName, clientId),
                    /* 2nd %s */ isJoin ? "joined" : "left",
                    /* 3rd %s */ roomName == null ? "" : roomName); 
            chatGamePanel.getChatPanel().addText(message);
            if (isJoin) {
                roomLabel.setText("Room: " + roomName);
                chatGamePanel.getChatPanel().addUserListItem(clientId, String.format("%s (%s)", clientName, clientId));
            } else {
                chatGamePanel.getChatPanel().removeUserListItem(clientId);
            }


        }
    }

    @Override
    public void onAwayStatus(long clientId, boolean isAway) {//vvh-12/09/24 Updates the away status for a client in the chat panel
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatGamePanel.getChatPanel().setUserAwayStatus(clientId, isAway);
        }
    }

    @Override
    public void onSpectateStatus(long clientId, boolean isSpectating) {//vvh-12/09/24 Updates the spectating status for a client in the chat panel
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatGamePanel.getChatPanel().setUserSpectateStatus(clientId, isSpectating);
        }
    }

    @Override
    public void onReceiveCategories(List<String> categories) {//vvh-12/09/24 Receives a list of categories and updates the ready panel in the game panel
//vvh-12/09/24        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatGamePanel.getGamePanel().readyPanel.categories(categories);
//vvh-12/09/24        }
    }

    @Override
    public void onCategorySelected(String category) {//vvh-12/09/24 Updates the ready panel with the selected category in the game panel
//vvh-12/09/24        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatGamePanel.getGamePanel().readyPanel.categorySelected(category);
//vvh-12/09/24        }
    }
    // Interface methods end
}