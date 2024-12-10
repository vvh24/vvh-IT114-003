package Project.Client.Interfaces;

public interface ISpectateEvents extends IGameEvents{
    /**
     * vvh-12/09/24 Called to update the spectating status of a client.
     *
     * @param clientId      The unique identifier of the client whose spectating status is being updated.
     * @param isSpectating  A boolean indicating whether the client is spectating (true) or not (false).
     */
    void onSpectateStatus(long clientId, boolean isSpectating);
}
