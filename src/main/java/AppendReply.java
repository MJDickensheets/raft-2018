public class AppendReply extends RPCMessage{

    private int id;
    private int reply;
    private boolean success;

    public AppendReply(int id, int reply, boolean success) {
        this.id = id;
        this.reply = reply;
        this.success = success;
    }

    @Override
    public String toString(){
        String s = "AppendRep ";
        s += id + " ";
        s += reply + " ";
        s+= success;
        return s;
    }
}
