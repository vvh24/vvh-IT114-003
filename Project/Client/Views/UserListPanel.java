package Project.Client.Views;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
    List<Long> orderOfComponents;

    /**
     * Constructor to create the UserListPanel UI.
     */
    public UserListPanel() {
        super(new BorderLayout(10, 10));
        userItemsMap = new HashMap<>(); // Initialize the map
        orderOfComponents = new ArrayList<>();

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
            //Add user item according to sorted key
            userItemsMap.put(clientId, userItem);
            int indexToPut = 0;
            for (int i = 0; i < orderOfComponents.size(); i++) {
                if (clientId < orderOfComponents.get(i)) {
                    indexToPut = i;
                } else {
                    indexToPut = i + 1;
                }
            }
            LoggerUtil.INSTANCE.info("Adding user to index: " + indexToPut);
            userListArea.add(userItem, gbc, indexToPut);
            orderOfComponents.add(indexToPut, clientId);
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
                orderOfComponents.remove(clientId);
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

    public void sortUserList() {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Sorting user list");
            userItemsMap.forEach((k, v) -> LoggerUtil.INSTANCE.info("User: " + k + " " + v));
            userListArea.removeAll();
            userItemsMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(e -> userListArea.add(e.getValue()));
            userListArea.add(Box.createVerticalGlue(), lastConstraints);
            userListArea.revalidate();
            userListArea.repaint();
        });
    }

    public void setUserAwayStatus(long clientId, boolean isAway) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setAwayStatus(isAway);
            });
        }
    }

    public void setUserSpectateStatus(long clientId, boolean isSpectating) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setSpectateStatus(isSpectating);
            });
        }
    }
}