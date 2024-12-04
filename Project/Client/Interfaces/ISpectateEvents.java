package Project.Client.Interfaces;

public interface ISpectateEvents extends IGameEvents{
    void onSpectateStatus(long clientId, boolean isSpectating);
}
