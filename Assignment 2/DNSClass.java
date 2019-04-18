
import java.util.HashMap;

class DNSClass {
    private HashMap<Integer,String> classTypes = new HashMap<>();

    DNSClass(){
        classTypes.put(1,"IN");
        classTypes.put(2,"CS");
        classTypes.put(3,"CH");
        classTypes.put(4,"HS");
    }

    String getClass(int value){
        return classTypes.get(value);
    }
}
