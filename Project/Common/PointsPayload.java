package Project.Common;

import java.util.Map; // vvh - 11/10/24 Importing Map to store player scores.
import Project.Common.LoggerUtil; // vvh - 11/10/24 Importing LoggerUtil for structured logging.

// vvh - 11/10/24 PointsPayload class, a specialized type of Payload for handling player scores.
public class PointsPayload extends Payload {
    private static final LoggerUtil logger = LoggerUtil.INSTANCE; // vvh - 11/11/24 LoggerUtil instance for logging debug information.

    // vvh - 11/10/24 Field specific to PointsPayload
    private Map<String, Integer> playerScores; // vvh - 11/10/24 Map to store each player's score, with player names as keys.

    // vvh - 11/10/24 Constructor for PointsPayload, initializing clientId, payloadType, message, and playerScores.
    public PointsPayload(long clientId, Map<String, Integer> playerScores) {
        setClientId(clientId); // vvh - 11/10/24 Set the client ID for this payload.
        setPayloadType(PayloadType.SCORE); // vvh - 11/10/24 Set the payload type to SCORE.
        setMessage("Score Update Payload"); //vvh - 11/10/24 Set a message indicating this is a score update.
        this.playerScores = playerScores; // vvh - 11/10/24 Assign the playerScores map to this instance.
    }

    // vvh - 11/10/24 Getter for playerScores field, returns the map of player scores.
    public Map<String, Integer> getPlayerScores() { 
        return playerScores; 
    }

    // vvh - 11/10/24 Override of the toString method to provide a structured string representation of the points payload.
    @Override
    public String toString() {
        // vvh - 11/10/24 Format the points payload details, including payload type, client ID, and player scores.
        String debugInfo = String.format(
            "PointsPayload[Type: %s, Client ID: %s, Scores: %s]", 
            getPayloadType(), getClientId(), playerScores
        );
        
        // vvh - 11/10/24 Log the formatted details of the points payload for debugging purposes.
        logger.info(debugInfo); // vvh - 11/10/24 Logs the points payload details at the INFO level.
        
        return debugInfo; // vvh - 11/10/24 Returns the formatted string for external use if needed.
    }
}
