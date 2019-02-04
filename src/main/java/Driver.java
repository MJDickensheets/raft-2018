import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
public class Driver {

    public static void main(String [] args) {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Scanner s = new Scanner(System.in);
        System.out.println("Server or client? (s/c)");
        String choice = s.next();

        if (choice.equals("s")) {
            System.out.println("Enter Node ID: ");
            int id = s.nextInt();


            Server self = new Server(id);

            /*
            if (id == 1) {
                System.out.println("Making node leader...");
                self.isLeader = true;
                self.isFollower = false;
                self.isCandidate = false;
            }*/

            boolean quit = false;
            boolean crashed = false;

            int timeout = 2;
            try {
                System.out.println("Type \"f\" to simulate a failure.\n" +
                        "Type \"r\" to simulate a recovery.\n" +
                        "Type \"t\" to simulate a timeout.\n" +
                        "Type \"q\" to quit");
                while (!quit) {
                    long startTime = System.currentTimeMillis();
                    if (!crashed) {
                        while ((System.currentTimeMillis() - startTime) < timeout * 1000 && !in.ready()) {
                            if (self.isFollower) {
                                self.wait_state();
                            } else if (self.isCandidate) {
                                self.election_state();
                            } else if (self.isLeader) {
                                self.leader_state();
                            }
                        }
                    }
                    if (in.ready()) {
                        String cmd = in.readLine();
                        if (cmd.equals("f") || cmd.equals("f\n")) {
                            crashed = true;
                            self.log.crash();
                        } else if (cmd.equals("r") || cmd.equals("r\n")) {
                            crashed = false;
                            self.queue.purgeQueue();
                            self.log.reboot();
                        } else if (cmd.equals("t") || cmd.equals("t\n")) {
                            self.isCandidate = true;
                            self.isLeader = false;
                            self.isFollower = false;
                        } else if (cmd.equals("q") || cmd.equals("q\n")) {
                            quit = true;
                        }
                    }

                }
            } catch (java.io.IOException e) {
                System.out.println("Caught IOException");
            }
        }
        else{
            System.out.println("Enter Node ID: ");
            int id = s.nextInt();


            Client self = new Client(id);
            boolean quit = false;
            int timeout = 1;

            try {
                while (!quit) {
                    long startTime = System.currentTimeMillis();
                    while ((System.currentTimeMillis() - startTime) < timeout * 1000 && !in.ready()) {
                        self.receive();
                    }
                    if (in.ready()) {
                        String cmd = in.readLine();
                        if(cmd.equals("q") || cmd.equals("q\n")){
                            self.sendEvent("PunchLeft");
                        }
                        else if(cmd.equals("w") || cmd.equals("w\n")){
                            self.sendEvent("PunchRight");
                        }
                        else if(cmd.equals("a") || cmd.equals("a\n")){
                            self.sendEvent("BlockLeft");
                        }
                        else if(cmd.equals("s") || cmd.equals("s\n")){
                            self.sendEvent("BlockRight");
                        }
                    }
                }
            }catch(java.io.IOException e){System.out.println("Caught IOException");}
        }
    }

}
