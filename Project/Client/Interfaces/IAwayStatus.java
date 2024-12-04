package Project.Client.Interfaces;

public interface IAwayStatus extends IGameEvents{
    void onAwayStatus(long clientId, boolean isAway);
}
