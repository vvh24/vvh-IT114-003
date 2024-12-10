package Project.Client.Views;

import java.awt.*;//vvh-12/09/24 importing awt classes 
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.*;//vvh-12/09/24 importing utility classes 
import java.util.List;//vvh-12/09/24 importing list 
import java.util.stream.Collectors;//vvh-12/09/24 importing collectors 

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import Project.Client.Client;
import Project.Client.ClientPlayer;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.LoggerUtil;

/**
 * UserListPanel represents a UI component that displays a list of users.
 */
public class UserListPanel extends JPanel implements IReadyEvent, IPointsEvent, ITurnEvent {
    private JPanel userListArea;
    private GridBagConstraints lastConstraints; // Keep track of the last constraints for the glue
    private HashMap<Long, UserListItem> userItemsMap; // Maintain a map of client IDs to UserListItems
    List<Long> orderOfComponents;//vvh-12/09/24 mantains the ordered list of the user IDs for sorting and displaying user list items 

    /**
     * Constructor to create the UserListPanel UI.
     */
    public UserListPanel() {
        super(new BorderLayout(10, 10));
        userItemsMap = new HashMap<>(); // Initialize the map
        orderOfComponents = new ArrayList<>();//vvh-12/09/24 initializes the list to store the order of user components 

        JPanel content = new JPanel(new GridBagLayout());
        userListArea = content;


        // Wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(userListArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0)); // Remove border

        this.add(scroll, BorderLayout.CENTER);

        userListArea.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        userListArea.revalidate();
                        userListArea.repaint();
                    });
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        userListArea.revalidate();
                        userListArea.repaint();
                    });
                }
            }
        });

        // Add vertical glue to push items to the top
        lastConstraints = new GridBagConstraints();
        lastConstraints.gridx = 0;
        lastConstraints.gridy = GridBagConstraints.RELATIVE;
        lastConstraints.weighty = 1.0;
        lastConstraints.fill = GridBagConstraints.VERTICAL;
        userListArea.add(Box.createVerticalGlue(), lastConstraints);

        // Listen for resize events to adjust user list items accordingly
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // SwingUtilities.invokeLater(() -> adjustUserListItemsWidth());
            }
        });
        // register to receive events
        Client.INSTANCE.addCallback(this);
    }

    /**
     * Adds a user to the list.
     *
     * @param clientId   The ID of the client.
     * @param clientName The name of the client.
     */
    protected void addUserListItem(long clientId, String clientName) {
        SwingUtilities.invokeLater(() -> {
            if (userItemsMap.containsKey(clientId)) {
                LoggerUtil.INSTANCE.warning("User already in the list: " + clientName);
                return; // User already in the list
            }

            LoggerUtil.INSTANCE.info("Adding user to list: " + clientName);

            UserListItem userItem = new UserListItem(clientId, clientName, userListArea);

            // GridBagConstraints settings for each user
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; // Column index 0
            gbc.gridy = userListArea.getComponentCount() - 1; // Place before the glue
            gbc.weightx = 1; // Let the component grow horizontally to fill the space
            gbc.anchor = GridBagConstraints.NORTH; // Anchor to the top
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 0); // Add spacing between users

            // Remove the last glue component if it exists
            if (lastConstraints != null) {
                int index = userListArea.getComponentCount() - 1;
                if (index > -1) {
                    userListArea.remove(index);
                }
            }
            //vvh-12/09/24 Add user item according to sorted key
            userItemsMap.put(clientId, userItem);
            int indexToPut = 0;//vvh-12/09/24 tracks the position to insert the new user based on sorted order 
            for (int i = 0; i < orderOfComponents.size(); i++) {//vvh-12/09/24 iterates through the list of current users
                if (clientId < orderOfComponents.get(i)) {
                    indexToPut = i; //vvh-12/09/24 insert before the current user 
                } else {
                    indexToPut = i + 1;//vvh-12/09/24 insert after the current user 
                }
            }
            LoggerUtil.INSTANCE.info("Adding user to index: " + indexToPut);//vvh-12/09/24 logs the determined index for adding the user 
            userListArea.add(userItem, gbc, indexToPut);
            orderOfComponents.add(indexToPut, clientId);//vvh-12/09/24 adds the client id to the list in the correct order 
            userListArea.add(Box.createVerticalGlue(), lastConstraints);
            userListArea.revalidate();
            userListArea.repaint();

        });
    }

    /**
     * Adjusts the width of all user list items.
     */
    private void adjustUserListItemsWidth() {
        SwingUtilities.invokeLater(() -> {
            for (UserListItem item : userItemsMap.values()) {
                item.setPreferredSize(
                        new Dimension(userListArea.getWidth() - 20, item.getPreferredSize().height));
            }
            userListArea.revalidate();
            userListArea.repaint();
        });
    }

    /**
     * Removes a user from the list.
     *
     * @param clientId The ID of the client to be removed.
     */
    protected void removeUserListItem(long clientId) {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Removing user list item for id " + clientId);
            UserListItem item = userItemsMap.remove(clientId); // Remove from the map
            if (item != null) {
                userListArea.remove(item);
                userListArea.revalidate();
                userListArea.repaint();
                orderOfComponents.remove(clientId);//vvh-12/09/24 removes the user id from the ordered list when a user is removed 
            }
        });
    }

    /**
     * Clears the user list.
     */
    protected void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Clearing user list");
            userItemsMap.clear(); // Clear the map
            userListArea.removeAll();
            userListArea.revalidate();
            userListArea.repaint();
            orderOfComponents.clear();
        });
    }

    @Override
    public void onTookTurn(long clientId, boolean didtakeCurn) {
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.values().forEach(u -> u.setTurn(false));// reset all
            });
        } else if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setTurn(didtakeCurn);
            });
        }
    }

    @Override
    public void onPointsUpdate(long clientId, int points) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                if (clientId > ClientPlayer.DEFAULT_CLIENT_ID) {
                    userItemsMap.get(clientId).setPoints(points);
                } else {
                    userItemsMap.values().forEach(u -> u.setPoints(-1));// reset all
                }
            });
        }
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.values().forEach(u -> u.setTurn(false));// reset all
            });
        } else if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setTurn(isReady, Color.GRAY);
            });
        }
    }

    public void sortUserList() {//vvh-12/09/24 sort user list based on client IDs 
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Sorting user list");
            userItemsMap.forEach((k, v) -> LoggerUtil.INSTANCE.info("User: " + k + " " + v));
            userListArea.removeAll();//vvh-12/09/24 clears all current components in the user list area 
            userItemsMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(e -> userListArea.add(e.getValue()));//vvh-12/09/24 adds sorted user items back to the user list area 
            userListArea.add(Box.createVerticalGlue(), lastConstraints);
            userListArea.revalidate();
            userListArea.repaint();
        });
    }

    public void setUserAwayStatus(long clientId, boolean isAway) {//vvh-12/09/24 updates the away status
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setAwayStatus(isAway);//vvh-12/09/24 updates the away status of the user in the user list item
            });
        }
    }

    public void setUserSpectateStatus(long clientId, boolean isSpectating) {//vvh-12/09/24 updates the spectating status
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setSpectateStatus(isSpectating);//vvh-12/09/24 updates the spectating status of the user in the user list item
            });
        }
    }
}