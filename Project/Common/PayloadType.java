package Project.Common;

public enum PayloadType {
    CLIENT_CONNECT, // client requesting to connect to server (passing of initialization data [name])
    CLIENT_ID,  // server sending client id
    SYNC_CLIENT,  // silent syncing of clients in room
    DISCONNECT,  // distinct disconnect action
    ROOM_CREATE,
    ROOM_JOIN, // join/leave room based on boolean
    MESSAGE, // sender and message,
    ROOM_LIST, // client: query for rooms, server: result of query,
    READY, // client to trigger themselves as ready, server to sync the related status of a particular client
    SYNC_READY, // quiet version of READY, used to sync existing ready status of clients in a GameRoom
    RESET_READY, // trigger to tell the client to reset their whole local list's ready status (saves network requests)
    PHASE, // syncs current phase of session (used as a switch to only allow certain logic to execute)
    TIME, // syncs current time of various timers
    EXAMPLE_TURN, // example of doing some turn logic

    ADD_QUESTION,//vvh-12/09/24 adding a new questio to the game
    AWAY,//vvh-12/09/24 marking player as away 
    NOT_AWAY,//vvh-12/09/24 marking player as not away 

    //vvh-11/10/24 Trivia-specific payload types
    QUESTION, // vvh-11/10/24 for sending questions and answer options to players
    POINTS, // vvh-11/10/24 for syncing points of players 
    ANSWER, //vvh- 11/10/24 for handling player answer choices 
    TIMER, 
    SPECTATE, //vvh-12/09/24 marker player as spectating
    NOT_SPECTATE, //vvh-12/09/24 marker player as not spectating 
    GET_CATEGORIES, //vvh-12/09/24 requesting a list of categories 
    CATEGORIES, //vvh-12/09/24 sending a list of categories to the client 
    SELECT_CATEGORY, //vvh-12/09/24 selecting a specific category 
    FETCH_CATEGORY,//vvh-12/09/24 fecthing the currently selected category 
}   