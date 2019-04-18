
import java.util.HashMap;

class DNSRecordType {
    private HashMap<Integer,String> recordType = new HashMap<>();

    DNSRecordType(){

        //specifying dns record types for output
        recordType.put(1,"A");
        recordType.put(2,"NS");
        recordType.put(3,"MD");
        recordType.put(4,"MF");
        recordType.put(5,"CNAME");
        recordType.put(6,"SOA");
        recordType.put(7,"MB");
        recordType.put(8,"MG");
        recordType.put(9,"MR");
        recordType.put(10,"NULL");
        recordType.put(11,"WKS");
        recordType.put(12,"PTR");
        recordType.put(13,"HIFO");
        recordType.put(14,"MINFO");
        recordType.put(15,"MX");
        recordType.put(16,"TXT");
        recordType.put(28,"AAAA");
    }

    String getRecordType(int value){
        return recordType.get(value);
    }
}
