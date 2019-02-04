public class RequestVote extends RPCMessage{

    private int term;
    private int candidateId;
    private int lastLogIndex;
    private int lastLogTerm;

    public RequestVote(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString(){
        String s = "RequestVote ";
        s += term + " ";
        s += candidateId + " ";
        s += lastLogIndex + " ";
        s += lastLogTerm;
        return s;
    }
}
