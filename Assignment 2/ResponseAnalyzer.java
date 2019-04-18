
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class ResponseAnalyzer {
    //creating instance of record type and class values helper class
    private static final DNSRecordType dnsRecordType = new DNSRecordType();
    private static final DNSClass dnsClass = new DNSClass();

    //Datagram packet to analyze
    private byte[] datagramPacketData;

    //additional helper instances
    private int answerCount;
    private int authorityRRCount;
    private int additionalRRCount;
    private int questionCount;
    private int aTypeAnswerCount;
    private int cNameAnswerCount;
    private int timeOutStatus;
    private int queryFor;

    private String queryId;
    private String flag;
    private String status;
    private String formatFlag;
    private String url;

    private ArrayList<Authority> authorityRRs = new ArrayList<>();
    private ArrayList<Additional> additionalRRs = new ArrayList<>();
    private ArrayList<Query> queries = new ArrayList<>();
    private ArrayList<String> cNames = new ArrayList<>();
    private ArrayList<String> nameServers = new ArrayList<>();
    private ArrayList<String> aTypeRecords = new ArrayList<>();
    private ArrayList<String> authorityAddress = new ArrayList<>();
    private static ArrayList<String> printableRecords = new ArrayList<>();

    //additional structure to store decompressed data
    private HashMap<Integer,String> savedCompressions = new HashMap<>();

    private DataInputStream dataInputStream;

    private String analyze() throws IOException {

        //IO streams for reading the packet
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(datagramPacketData);
        dataInputStream = new DataInputStream(byteArrayInputStream);

        //storing data into fields
        queryId = "0x"+String.format("%x",dataInputStream.readShort());
        short flagInt = dataInputStream.readShort();
        flag = "0x"+String.format("%x",flagInt);

        //response fields
        questionCount = dataInputStream.readShort();
        answerCount = dataInputStream.readShort();
        authorityRRCount = dataInputStream.readShort();
        additionalRRCount = dataInputStream.readShort();

        //getting response status
        formatFlag = Integer.toBinaryString(flagInt);
        formatFlag = formatFlag.substring(16,formatFlag.length()-1);
        if(Integer.toBinaryString(flagInt).endsWith("0011")){
            status = "NXDOMAIN";
        }
        else{
            status = "NOERROR";
        }

        //storing query description
        for (int j = 0; j < questionCount; j++) {
            //helper data class object
            Query query = new Query();
            query.domainName = parseName();
            query.type = dnsRecordType.getRecordType(dataInputStream.readShort());
            query.recordClass = dnsClass.getClass(dataInputStream.readShort());
            queries.add(query);
        }
        if(!status.equals("NXDOMAIN")) {
            //parsing answer section
            for (int j = 0; j < answerCount; j++) {
                //helper data class object
                Answer answer = new Answer();
                answer.domainName = parseName();
                answer.type = dnsRecordType.getRecordType(dataInputStream.readShort());
                answer.recordClass = dnsClass.getClass(dataInputStream.readShort());
                answer.ttl = Integer.toString(dataInputStream.readInt());
                if (answer.type.equals("A")) {
                    int ipLen = dataInputStream.readShort();
                    int[] ip = new int[4];
                    for (int i = 0; i < ipLen; i++) {
                        ip[i] = dataInputStream.readUnsignedByte();
                    }
                    answer.address = String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
                    aTypeAnswerCount++;
                    aTypeRecords.add(answer.address);
                    answer.cname = null;
                } else if (answer.type.equals("CNAME")) {
                    dataInputStream.readShort();
                    answer.cname = parseName();
                    saveCNames(answer.cname);
                    cNameAnswerCount++;
                    answer.address = null;
                }
                if(queryFor == 0) {
                    setPrintableRecords(answer.toString());
                }
            }

            //parsing authority section
            for (int j = 0; j < authorityRRCount; j++) {
                //helper data class object
                Authority authority = new Authority();
                authority.domainName = parseName();
                authority.type = dnsRecordType.getRecordType(dataInputStream.readShort());
                authority.recordClass = dnsClass.getClass(dataInputStream.readShort());
                authority.ttl = Integer.toString(dataInputStream.readInt());
                dataInputStream.readShort();
                authority.nameServer = parseName();
                nameServers.add(authority.nameServer);
                authorityRRs.add(authority);
            }

            //parsing additional section
            for (int j = 0; j < additionalRRCount; j++) {
                //helper data class object
                Additional additional = new Additional();
                additional.domainName = parseName();
                additional.type = dnsRecordType.getRecordType(dataInputStream.readShort());
                additional.recordClass = dnsClass.getClass(dataInputStream.readShort());
                additional.ttl = Integer.toString(dataInputStream.readInt());
                int ipLen = dataInputStream.readShort();
                if (additional.type.equals("A")) {
                    int[] ip = new int[4];
                    for (int i = 0; i < ipLen; i++) {
                        ip[i] = dataInputStream.readUnsignedByte();
                    }
                    additional.address = String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
                    authorityAddress.add(additional.address);
                    additional.aaaaAddress = null;
                } else if (additional.type.equals("AAAA")) {
                    additional.aaaaAddress = "";
                    additional.aaaaAddress += String.format("%x:", dataInputStream.readUnsignedShort());
                    additional.aaaaAddress += String.format("%x:", dataInputStream.readUnsignedShort());
                    additional.aaaaAddress += String.format("%x:", dataInputStream.readUnsignedShort());
                    dataInputStream.readShort();
                    dataInputStream.readShort();
                    dataInputStream.readShort();
                    additional.aaaaAddress += String.format(":%x:", dataInputStream.readUnsignedShort());
                    additional.aaaaAddress += String.format("%x", dataInputStream.readUnsignedShort());
                }
                additionalRRs.add(additional);
            }
        }

        return status;
    }

    String parse(byte[] datagramPacketData,String url) throws IOException {
        this.datagramPacketData = datagramPacketData;
        this.url = url;
        return analyze();
    }

    //showing the content of the packet
    void showPacket(String domain){
        System.out.println("; <<>> DNS_resolver <<>> "+domain);
        System.out.println(";; Got answer:");
        System.out.println(";; ->>HEADER<<- opcode: QUERY, status: "+status+", id: "+queryId);
        System.out.print(";; flags: qr");
        if(formatFlag.charAt(8)=='1') System.out.print(" ra");
        System.out.println("; QUERY: "+questionCount+", ANSWER: "+printableRecords.size()+", AUTHORITY: "+authorityRRCount+", ADDITIONAL: "+additionalRRCount);
        System.out.println();
        System.out.println(";; OPT PSEUDOSECTION:");
        System.out.println("; EDNS: flags:"+flag+";");

        //printing query section
        System.out.println(";; QUESTION SECTION:");
        for(Query query : queries){
            query.domainName = domain;
            query.type = dnsRecordType.getRecordType(1);
            query.recordClass = dnsClass.getClass(1);
            System.out.println(query.toString());
        }
        System.out.println();

        //printing answer section
        if(!status.equals("NXDOMAIN")) {
            if (answerCount > 0) {
                System.out.println(";; ANSWER SECTION:");
                for (String string : printableRecords) {
                    System.out.println(string);
                }
                System.out.println();
            }

            if (authorityRRCount > 0 && answerCount==0) {
                System.out.println(";; AUTHORITY SECTION:");
                for (Authority authority : authorityRRs) {
                    System.out.println(authority.toString());
                }
                System.out.println();
            }

            if (additionalRRCount > 0 && answerCount==0) {
                System.out.println(";; ADDITIONAL SECTION:");
                for (Additional additional : additionalRRs) {
                    System.out.println(additional.toString());
                }
                System.out.println();
            }
        }

        Date date = new Date();
        String strDateFormat = "E MMM dd HH:mm:ss Z yyyy";
        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        String formattedDate= dateFormat.format(date);
        System.out.println(";; WHEN: "+formattedDate);
        List<Integer> iList = new ArrayList<>();

        for (int i : datagramPacketData) {
            iList.add(i);
        }
        System.out.println(";; MSG SIZE  rcvd: "+ Collections.frequency(iList, 0)+" BYTES");
    }

    //method for parsing name sections
    private String parseName() throws IOException {
        String name = "";
        int len;
        while((len = dataInputStream.readUnsignedByte())!=0) {
            if(len >= 192){
                int startIndex = dataInputStream.readUnsignedByte();
                if(len>192){
                    startIndex+=256*(len-192);
                }
                if(savedCompressions.get(startIndex) == null){
                    savedCompressions.put(startIndex,decompress(startIndex)+ ".");
                    name += savedCompressions.get(startIndex);
                }
                else {
                    name += savedCompressions.get(startIndex);
                }
                break;
            }
            else {
                byte[] domainPart = new byte[len];
                for (int i = 0; i < len; i++) {
                    domainPart[i] = dataInputStream.readByte();
                }
                name += new String(domainPart, StandardCharsets.UTF_8) + ".";
            }
        }
        name = name.substring(0,name.length()-1);
        return name;
    }

    private String decompress(int startIndex){
        String name = "";
        int len;
        while((len = toUnsignedByte(datagramPacketData[startIndex]))!=0) {
            if(len >= 192){
                int index = toUnsignedByte(datagramPacketData[startIndex+1]);
                if(len>192){
                    index+=256*(len-192);
                }
                if(savedCompressions.get(index) == null){
                    savedCompressions.put(index,decompress(index)+ ".");
                    name += savedCompressions.get(index);

                }
                else {
                    name += savedCompressions.get(index);
                }
                break;
            }
            else {
                byte[] domainPart = new byte[len];
                for (int i = 0; i < len; i++) {
                    domainPart[i] = datagramPacketData[++startIndex];
                }
                startIndex++;
                name += new String(domainPart, StandardCharsets.UTF_8) + ".";
            }
        }
        name = name.substring(0,name.length()-1);
        return name;
    }

    void showTimeoutMessage(String domain){
        System.out.println("; <<>> DNS_resolver <<>> "+domain);
        System.out.println(";; connection timed out; no servers could be reached");
    }

    int getaTypeAnswerCount() {
        return aTypeAnswerCount;
    }

    private int toUnsignedByte(byte b) {
        return b & 0xFF;
    }
    
    private void saveCNames(String cname){
    	cNames.add(cname);
    }
    
    ArrayList<String> getCNames(){
    	return cNames;
    }

    ArrayList<String> getAuthorityAddress(){return authorityAddress;}
    
    private void setPrintableRecords(String record){
    	printableRecords.add(record);
    }

    void setTimeOutStatus(int timeOutStatus){
        this.timeOutStatus = timeOutStatus;
    }

    int getTimeOutStatus() {
        return timeOutStatus;
    }

    int getCNameAnswerCount() {
        return cNameAnswerCount;
    }

    ArrayList<String> getNameServers(){
        return nameServers;
    }

    ArrayList<String> getATypeRecords(){
        return aTypeRecords;
    }

    void setQueryFor(int number){
        queryFor = number;
    }

    //query data class
    private class Query{
        String domainName;
        String type;
        String recordClass;

        public String toString(){
            String description = ";";
            description+=String.format("%-50s %-15s %-15s",domainName,recordClass,type);
            return description;
        }
    }

    //answer data class
    private class Answer{
        String domainName;
        String type;
        String recordClass;
        String address;
        String cname;
        String ttl;

        public String toString(){
            String description = ";";
            description+=String.format("%-50s %-15s %-15s %-15s",domainName,ttl,recordClass,type);
            if(type.equals("CNAME")){
                description+=String.format("%-15s",cname);
            }
            else if(type.equals("A")){
                description+=String.format("%-15s",address);
            }
            return description;
        }
    }

    //authoritative data class
    private class Authority{
        String domainName;
        String type;
        String recordClass;
        String nameServer;
        String ttl;

        public String toString() {
            String description = ";";
            description+=String.format("%-50s %-15s %-15s %-15s %-30s",domainName,ttl,recordClass,type,nameServer);
            return description;
        }
    }

    //additional data class
    private class Additional{
        String domainName;
        String type;
        String recordClass;
        String address;
        String aaaaAddress;
        String ttl;

        public String toString() {
            String description = ";";
            description+=String.format("%-50s %-15s %-15s %-15s",domainName,ttl,recordClass,type);
            if(type.equals("A")) {
                description += String.format("%-15s",address);
            }
            else if(type.equals("AAAA")){
                description += String.format("%-15s",aaaaAddress);
            }
            return description;
        }
    }
}
