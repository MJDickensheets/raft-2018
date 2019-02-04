public class LogEntry {


    private int term;
    private int id;
    private String contents;

    public LogEntry(int term, int id,  String contents){
        this.term = term;
        this.id = id;
        this.contents = contents;
    }

    public int getId() {
        return id;
    }

    public String getContents() {
        return contents;
    }

    public int getTerm() {
        return term;
    }

    public String toString(){
        String s = "";
        s += term + " ";
        s += id + " ";
        s += contents;
        return s;
    }
}
