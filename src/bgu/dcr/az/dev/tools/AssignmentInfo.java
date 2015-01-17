package bgu.dcr.az.dev.tools;

/**
 * Created by ChrisQin on 10/9/2014.
 */
public class AssignmentInfo {
    private int value;
    private int ID;

    public int getValue(){
        return value;
    }

    public int getID(){
        return ID;
    }

    public AssignmentInfo(int _value, int _ID){
        this.value = _value;
        this.ID = _ID;
    }
}
