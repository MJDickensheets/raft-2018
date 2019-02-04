public class ReqestReply extends RPCMessage{

    private int term;
    private boolean voteGranted;

    public ReqestReply(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString(){
        String s = "RequestRep ";
        s += term + " ";
        s += voteGranted;
        return s;
    }
}
