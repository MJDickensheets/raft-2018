import java.util.ArrayList;

public class AppendEntries extends RPCMessage{
    private int term;
    private int leaderId;
    private int prevLogIndex;
    private int prevLogTerm;

    private int leaderCommit;
    private ArrayList<LogEntry> entries;

    public AppendEntries(int term, int leaderId, int prevLogIndex, int prevLogTerm, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.leaderCommit = leaderCommit;
        this.entries = new ArrayList<>();
    }

    public void addEntry(LogEntry e){
        entries.add(e);
    }

    @Override
    public String toString(){
        String s = "Append ";
        s += term + " ";
        s += leaderId + " ";
        s += prevLogIndex + " ";
        s += prevLogTerm + " ";
        s += leaderCommit + " ";
        s += entries.size() + " ";
        for(int i = 0; i < entries.size(); i++){
            s += entries.get(i).toString() + " ";
        }
        return s;
    }
}
