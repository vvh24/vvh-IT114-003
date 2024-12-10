package Project.Common;

public class GetCategoriesPayload extends Payload {
    private String room;

    public GetCategoriesPayload() { //vvh-12/09/24 Constructor to initialize the payload type as GET_CATEGORIES
        setPayloadType(PayloadType.GET_CATEGORIES);
    }
    /**
     * vvh - 12/09/24 Sets the room name in the payload.
     *
     * @param room The name of the room for which categories are requested.
     */
    public void setRoom(String room) {
        this.room = room;
    }
}
