import java.util.Scanner;
public class Client {

    private int clientId;
    private int leader;

    private AWSClient queue;
    private String urlBase = "https://sqs.us-east-1.amazonaws.com/150940258986/Queue";

    public Client(int clientId){
        this.clientId = clientId;
        this.leader = 1;
        this.queue = new AWSClient(clientId);
        queue.createQueue();
    }

    public void sendEvent(String contents){
        ClientMessage m = new ClientMessage(clientId, contents);
        queue.sendMessage(urlBase + leader, m.toString());
    }

    public void receive(){
        String msg = queue.receiveMessage();
        if(msg != null) {
            Scanner s = new Scanner(msg);
            leader = s.nextInt();
            System.out.println(s.next());
        }
    }
}
