public class ClientMessage extends RPCMessage {

    int id;
    String contents;

    public ClientMessage(int id, String contents){
        this.id = id;
        this.contents = contents;
    }

    @Override
    public String toString(){
        return "Client " + id + " " + contents;
    }
}
