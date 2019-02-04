import java.util.Random;

public class StateMachine {
    //States: 0 for no block, 1 for left block, 2 for right block
    private int state;

    public StateMachine(){
        this.state = 0;
    }


    public void changeState(int newState){
        state = newState;
    }

    public int getState() {
        return state;
    }

    public boolean isHit(String direction){
        Random r = new Random();
        if(direction.equals("PunchLeft") && (state == 0 || state == 2)){
            if(r.nextFloat() < 0.1) return true;
        }
        else if(direction.equals("PunchRight") && (state == 0 || state == 1)){
            if(r.nextFloat() < 0.1) return true;
        }
        return false;
    }

    public String applyState(String command){
        String replyMsg = "";
        if(command.equals("PunchLeft")){
            state = 0;
            if(isHit("PunchLeft")) replyMsg += "HIT";
            else replyMsg += "MISSED";
        }
        else if(command.equals("PunchRight")){
            state = 0;
            if(isHit("PunchRight")) replyMsg += "HIT";
            else replyMsg += "MISSED";
        }
        else if(command.equals("BlockLeft")){
            state = 1;
            replyMsg += "Blocking Left";
        }
        else if(command.equals("BlockRight")){
            state = 2;
            replyMsg += "Blocking Right";
        }
        return replyMsg;
    }
}
