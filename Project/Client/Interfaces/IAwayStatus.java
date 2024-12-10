package Project.Client.Interfaces;
//vvh - 12/09/24 Interface that extends IGameEvents to handle "away" status events for clients
public interface IAwayStatus extends IGameEvents{
    void onAwayStatus(long clientId, boolean isAway);
}
