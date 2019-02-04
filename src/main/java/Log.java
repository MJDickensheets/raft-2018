import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

public class Log {

    private ArrayList<LogEntry> log;
    File log_file;
    String filename;
    PrintWriter writer;

    public Log(int id){
        this.filename = "./log" + id + ".txt";
        this.log_file = new File(filename);
        this.log_file.deleteOnExit();
        log = new ArrayList<>();
    }


    public void addEntry(int term, LogEntry e){

        log.add(e);
        write_to_file();
    }

    public ArrayList<LogEntry> getLog() {
        return log;
    }

    public int getLastIndex(){
        if(log.isEmpty()) return -1;
        return log.size() - 1;
    }

    public int getLastTerm(){
        if(getLastIndex()==-1) return -1;
        return log.get(getLastIndex()).getTerm();
    }

    public boolean hasEntry(int index, int term){
        if(index > -1 && index < log.size()) {
            return (log.get(index).getTerm() == term);
        }
            return false;
    }

    public int getSize(){
        return log.size()-1;
    }

    public void delete(int index){
        log = new ArrayList<LogEntry>( log.subList(0, index+1));
    }

    public void crash(){
        log.clear();
    }

    public void reboot(){
        try {
            Scanner s = new Scanner(log_file);
            while(s.hasNext()){
                int term = s.nextInt();
                int id = s.nextInt();
                String contents = s.next();
                addEntry(term, new LogEntry(term, id, contents));
            }
        }catch(FileNotFoundException e){}
    }

    private void initialize_file(){
        log_file = new File(filename);
        try{
            writer = new PrintWriter(log_file);
        }catch(FileNotFoundException e){}
    }

    private void write_to_file(){
        log_file.delete();
        initialize_file();
        for(int i = 0; i < log.size(); i ++){
            writer.println(log.get(i).toString());
            writer.flush();
            System.out.println(log.get(i).toString());
        }
    }
}
