import java.util.Random;
import java.util.Scanner;

public class Server {

    private int serverId;

    public boolean isLeader;
    private Integer leader;
    public boolean isCandidate;
    public boolean isFollower;
    private int currentTerm;
    private Integer votedFor;
    private int voteCount;

    private int commitIndex;
    private int lastApplied;

    private int[] nextIndex;
    private int[] matchIndex;
    int prevLogIndex;
    int prevLogTerm;


    private long time = System.currentTimeMillis();
    private long timeout;
    private long old_time = time;
    Random ran = new Random(time);

    public AWSClient queue;
    private String urlBase = "https://sqs.us-east-1.amazonaws.com/150940258986/Queue";

    public Log log;
    private StateMachine stateBlue;
    private StateMachine stateRed;

    public Server(int serverId){
        this.serverId = serverId;
        this.isLeader = false;
        this.leader = -1;
        this.isCandidate = false;
        this.isFollower = true;
        this.currentTerm = 0;
        this.votedFor = null;
        this.voteCount = 0;

        this.commitIndex=-1;
        this.lastApplied = -1;

        //Volitile states for leader
        nextIndex = new int[]{0,0,0,0,0};
        matchIndex = new int[]{0,0,0,0,0};
        this.prevLogIndex = -1;
        this.prevLogTerm = -1;

        this.queue = new AWSClient(serverId);
        queue.createQueue();

        this.log = new Log(serverId);
        this.stateBlue = new StateMachine();
        this.stateRed = new StateMachine();

    }

    //define waite state, check for timeout
    public void wait_state(){
        if(leader != -1) System.out.println("Following leader " + leader);
        else System.out.println("No leader currently...");
        receive();

        if(check_timeout()){
            makeCandidate();
        }

        System.out.println("Log Size: " + (log.getSize()+1));

    }


    public boolean check_timeout(){
        updateTime();
        if(time - old_time > timeout){
            System.out.println("Timed out...");
            return true;
        }
        return false;
    }

    public void election_state(){
        voteCount = 1;
        votedFor = serverId;
        currentTerm++;
        sendMessage(new RequestVote(currentTerm, serverId, log.getLastIndex(),
                log.getLastTerm()), true, null);
        updateTime();
        long startTime = time;
        long elecTimeout = (long)ran.nextFloat()*3000 + 4000;
        System.out.println("Started Election");
        while(time - startTime < elecTimeout) {
            receive();
            System.out.println("Waiting for Votes");
            if (isFollower) return;
            //if win election
            if (voteCount == 3) {
                System.out.println("Becoming new leader...");
                makeLeader();
                leader = serverId;
                //update_volitiles();
                return;
            }
            updateTime();
        }
        votedFor = null;
    }

    public void leader_state(){
        receive();
        if(isFollower) return;
        sendHeartbeat();

        while(!is_uptodate()){

            for(int i = 0; i < 5; i++) {
                prevLogIndex = nextIndex[i] -1;
                if(prevLogIndex > -1) prevLogTerm = log.getLog().get(prevLogIndex).getTerm();
                else prevLogTerm = -1;
                AppendEntries message = new AppendEntries(currentTerm, serverId, prevLogIndex, prevLogTerm, commitIndex);
                if (i + 1 != serverId) {
                    if(nextIndex[i] >= matchIndex[i]) {
                        for (int j = nextIndex[i]; j <= log.getLastIndex(); j++) {

                            message.addEntry(log.getLog().get(j));
                            System.out.println("Added log entry to message");
                        }
                        sendMessage(message, false, i + 1);
                        System.out.println("Sent append message to node " + (i + 1));
                    }

                }
                receive();
            }
            receive();
            apply();
            if(isFollower) return;
        }
    }

    public boolean is_uptodate(){
        for(int i = 0; i < 5; i++){
            if(matchIndex[i]<nextIndex[i]) return false;
        }
        return true;
    }

    public void update_committed(){
        int lowest = Integer.MAX_VALUE;
        int second = Integer.MAX_VALUE;
        int third = Integer.MAX_VALUE;
        for(int i = 0; i < 5; i++){
            if(matchIndex[i] < lowest){
                third = second;
                second = lowest;
                lowest = matchIndex[i];
            }
            else if(matchIndex[i] < second){
                third = second;
                second = matchIndex[i];
            }
            else if(matchIndex[i]<third){
                third = matchIndex[i];
            }
        }
        commitIndex = Math.min(third, log.getSize());
        System.out.println("New Commit Index = " + commitIndex);

    }

    public void update_volitiles(){
        // reinitilaize volitile states
        int index = log.getLog().size()-1;
        for (int i = 0; i < 5; i++){
            nextIndex[i] = index;
            if(i+1 == serverId)matchIndex[i]=index;
            else matchIndex[i] = -1;
        }
        //for(int i = 0; i < 5; i++){
        //    System.out.print(matchIndex[i] + " " + nextIndex[i] + " ");
        //}
    }

    public void sendHeartbeat(){
        System.out.println("Sending Heartbeat... Term = " + currentTerm);
        AppendEntries m = new AppendEntries(currentTerm, serverId, 0, 0, commitIndex);
        sendMessage(m, true, null);
    }

    public void apply(){
        if(commitIndex > lastApplied){
            lastApplied++;
            //APPLY log[last_applied]
            //Red robot has id 6, blue has id 7
            int id = log.getLog().get(lastApplied).getId();
            System.out.println("APPLYING ACTION TO NODE: " + id);
            String command = log.getLog().get(lastApplied).getContents();
            String reply = "";
            if(id == 6){
                if(command.equals("PunchLeft") || command.equals("PunchRight")){
                    reply += stateBlue.applyState(command);
                }
                else reply += stateRed.applyState(command);
            }
            else{
                if(command.equals("PunchLeft") || command.equals("PunchRight")){
                    reply += stateRed.applyState(command);
                }
                else reply += stateBlue.applyState(command);
            }
            if(isLeader) {
                System.out.println("Sending Message to Client...");
                if (reply.equals("HIT")) {
                    queue.sendMessage(urlBase + 6, serverId + " " + reply);
                    queue.sendMessage(urlBase + 7, serverId + " " + reply);
                } else queue.sendMessage(urlBase + id, serverId + " " + reply);
            }
        }
    }

    public void sendMessage(RPCMessage m, boolean broadcast, Integer recipient){
        if(broadcast){
            for(int i = 1; i < 6; i++) {
                if(i != serverId) queue.sendMessage(urlBase + i, m.toString());
            }
        }
        else{
            queue.sendMessage(urlBase+recipient, m.toString());
        }
    }

    public void receive(){

        String contents = queue.receiveMessage();
        if(contents!=null){
            Scanner parse = new Scanner(contents);
            String type = parse.next();
            System.out.println("Received RPC of type " + type);
            if(type.equals("Append")){
                int term = parse.nextInt();
                int leaderId = parse.nextInt();
                int prevLogIndex = parse.nextInt();
                int prevLogTerm = parse.nextInt();
                int leaderCommit = parse.nextInt();
                int size = parse.nextInt();



                // update current term and become follower
                if(term >= currentTerm){
                    updateTerm(term);
                    if(leader != leaderId) {
                        System.out.println("New Leader: " + leaderId);
                        leader = leaderId;
                    }
                }

                // check if heartbeat
                if(size == 0){
                    System.out.println("Received Heartbeat");
                    updateTimeout();
                    return;
                }

                if(term < currentTerm){
                    System.out.println("Denying append because of outdated term");
                    sendMessage(new AppendReply(serverId, currentTerm, false), true, null);
                    return;
                }
                else if(!log.hasEntry(prevLogIndex, prevLogTerm)&&prevLogIndex != -1){
                    System.out.println("Denying append because of lack of previous term");
                    sendMessage(new AppendReply(serverId, currentTerm, false), true, null);
                    return;
                }
                //delete entries after new log start

                if(prevLogIndex > -1)log.delete(prevLogIndex);


                System.out.println("Adding new log entries from leader " + leaderId);
                for(int i = 0; i < size; i++){
                    int e_term = parse.nextInt();
                    int e_id = parse.nextInt();
                    String e_command = parse.next();
                    //check for duplicate first entries
                    //if(prevLogIndex == -1 && log.getLastTerm()==e_term){
                       //return;
                   // }
                    System.out.println(prevLogIndex);
                    if(!log.hasEntry(prevLogIndex+i+1, e_term)) {
                        System.out.println("Added term at index: " + prevLogIndex+i+1);
                        LogEntry e = new LogEntry(e_term, e_id, e_command);
                        log.addEntry(e_term, e);
                        //System.out.println("Successfully appended. Sending reply");

                    }

                    if(i == size-1){
                        if(leaderCommit > commitIndex){
                            commitIndex = Math.min(leaderCommit, log.getLog().size());
                            apply();
                        }
                        sendMessage(new AppendReply(serverId, currentTerm, true), false, leaderId);
                    }
                }


                //restart timeout
                System.out.println("Restarting Timeout");
                updateTimeout();

            }
            else if(type.equals("AppendRep")){
                if(isLeader){
                    int id = parse.nextInt();
                    int term = parse.nextInt();
                    boolean success = parse.nextBoolean();

                    // update current term and become follower
                    if(term > currentTerm) {
                        updateTerm(term);
                        return;
                    }

                    if(success){
                        matchIndex[id-1]++;
                        //System.out.println("AppendSuccess received. Current matchIndex vs nextIndex:");
                        //for(int i = 0; i < 5; i++){
                        //    System.out.print(matchIndex[i] + " " + nextIndex[i] + " ");
                        //}
                        update_committed();
                    }
                    else{
                        if(nextIndex[id-1] > 0) nextIndex[id-1]--;
                        //System.out.println("AppendDeny received. Current matchIndex vs nextIndex:");
                        //for(int i = 0; i < 5; i++){
                        //    System.out.print(matchIndex[i] + " " + nextIndex[i] + " ");
                        //}
                    }
                }

            }
            else if(type.equals("RequestVote")){
                int term = parse.nextInt();
                int candidateId = parse.nextInt();
                int lastLogIndex = parse.nextInt();
                int lastLogTerm = parse.nextInt();

                // update current term and become follower
                if(term > currentTerm) {
                    updateTerm(term);
                }

                //catch null pointers for nodes that miss messages
                if(lastLogIndex > log.getSize()) return;

                if(term < currentTerm){
                    sendMessage(new ReqestReply(currentTerm, false), false, candidateId);
                    return;
                }
                else if((votedFor == null || votedFor == candidateId)){
                    if(log.getLastIndex()==-1 || lastLogIndex == -1 || log.getLog().get(lastLogIndex).getTerm()==lastLogTerm ){
                        sendMessage(new ReqestReply(currentTerm, true), false, candidateId);
                        updateTimeout();
                    }
                }

            }
            else if(type.equals("RequestRep")){
                int term = parse.nextInt();
                boolean vote = parse.nextBoolean();

                // update current term and become follower
                if(term > currentTerm) updateTerm(term);

                // on vote successful vote receipt
                if(vote && term==currentTerm) {
                    voteCount++;
                    System.out.println("Received vote");
                }
            }
            else if(type.equals("Client")){
                int id = parse.nextInt();
                String command = parse.next();
                if(isLeader){
                    prevLogIndex = log.getLastIndex();
                    prevLogTerm = log.getLastTerm();
                    LogEntry e = new LogEntry(currentTerm, id,  command);
                    log.addEntry(currentTerm, e);
                    nextIndex[serverId-1]++;
                    update_volitiles();

                }
                else if(leader != -1){
                    sendMessage(new ClientMessage(id, command), false, leader);
                }
            }
        }
    }

    public void updateTerm(int term){
        if(term > currentTerm) System.out.println("Updating term: " + term);
        currentTerm = term;
        votedFor = null;

        updateTimeout();
        makeFollower();
    }

    public void makeLeader(){
        isLeader = true;
        isCandidate = false;
        isFollower = false;
    }

    public void makeCandidate(){
        isLeader = false;
        isCandidate = true;
        isFollower = false;
    }

    public void makeFollower(){
        isLeader = false;
        isCandidate = false;
        isFollower = true;
        leader = -1;
    }

    public void updateTime(){
        this.time = System.currentTimeMillis();
    }

    public void updateTimeout(){
        this.timeout = (long)ran.nextFloat()*6000 + 10000;
        updateTime();
        old_time = time;
    }


}
