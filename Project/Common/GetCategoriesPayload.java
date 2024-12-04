package Project.Common;

public class GetCategoriesPayload extends Payload {
    private String room;

    public GetCategoriesPayload() {
        setPayloadType(PayloadType.GET_CATEGORIES);
    }

    public void setRoom(String room) {
        this.room = room;
    }
}
