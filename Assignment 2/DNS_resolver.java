
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DNS_resolver {

    //helper instances
    private static String domainName;
    private final static String ROOT_ADDRESS = "192.58.128.30";
    private static HashMap<String,String> savedNameServers = new HashMap<>();

    //function for iterative query
    private static ResponseAnalyzer runIteration(String address,int type) throws IOException{

        ResponseAnalyzer responseAnalyzer = null;
            try {
                //DatagramSocket object for sending and receiving UDP packets and setting timeouts
                DatagramSocket datagramSocket = new DatagramSocket();
                datagramSocket.setSoTimeout(5000);

                //creating a Query builder helper class object and building query
                QueryBuilder queryBuilder = new QueryBuilder(domainName);
                queryBuilder.build();
                byte[] queryFrame = queryBuilder.queryToByteArray();

                //creating datagram packet
                DatagramPacket datagramPacket = new DatagramPacket(queryFrame,queryFrame.length,new InetSocketAddress(address,53));
    
                //sending datagram packet
                datagramSocket.send(datagramPacket);
    
                //receiving datagram response packet
                byte[] response = new byte[2048];
                DatagramPacket responsePacket = new DatagramPacket(response,response.length);

                try {
                    //receiving packet and closing socket
                    datagramSocket.receive(responsePacket);
                    datagramSocket.close();
    
                    //analysing response
                    responseAnalyzer = new ResponseAnalyzer();

                    //setting type for query, this type indicates whether the query is done for
                    //nameserver address or the domain ip address
                    responseAnalyzer.setQueryFor(type);
                    if(responseAnalyzer.parse(response,domainName).equals("NXDOMAIN")){
                        //no such domain check
                        return responseAnalyzer;
                    }

                    else if(responseAnalyzer.getaTypeAnswerCount()>0){
                        //domain ip found
                        return responseAnalyzer;
                    }

                    else if(responseAnalyzer.getCNameAnswerCount()>0){
                        //commencing queries for cname records
                        ArrayList<String> cNames = responseAnalyzer.getCNames();
                        domainName = cNames.get(0);
                        return runIteration(ROOT_ADDRESS,type);
                    }

                    else if((responseAnalyzer.getaTypeAnswerCount()+responseAnalyzer.getCNameAnswerCount())==0){
                        //if the ip is not found in this dns address commence query for name servers
                        ArrayList<String> addresses = responseAnalyzer.getAuthorityAddress();
                        if(addresses.size()>0){
                            for(String ip: addresses){
                                responseAnalyzer = runIteration(ip,type);
                                if(responseAnalyzer.getaTypeAnswerCount()>0){
                                    return responseAnalyzer;
                                }
                            }
                        }

                        else{
                            //if the ip address of the nameservers is not provided in the additional section
                            String mainDomain = domainName;
                            ArrayList<String> nameServers = responseAnalyzer.getNameServers();
                            for(String nameServer : nameServers){
                                if(savedNameServers.get(nameServer)==null){
                                    domainName = nameServer;
                                    responseAnalyzer = runIteration(ROOT_ADDRESS,1);
                                    if(responseAnalyzer.getaTypeAnswerCount()>0){
                                        ArrayList<String> ipList = responseAnalyzer.getATypeRecords();
                                        for(String ip : ipList){
                                            //storing ip address for efficiency
                                            savedNameServers.put(nameServer,ip);
                                        }
                                    }
                                    domainName = mainDomain;
                                }

                                responseAnalyzer = runIteration(savedNameServers.get(nameServer),type);
                                if(responseAnalyzer.getaTypeAnswerCount()>0){
                                    return responseAnalyzer;
                                }
                            }
                        }
                    }
                } catch (SocketTimeoutException e){
                    //throwing socket timeout
                    responseAnalyzer = new ResponseAnalyzer();
                    responseAnalyzer.setTimeOutStatus(1);
                    return responseAnalyzer;
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        return responseAnalyzer;
    }

    public static void main(String[] args) throws IOException {

        //adding domain name
        domainName = args[0];
        ResponseAnalyzer responseAnalyzer = runIteration(ROOT_ADDRESS,0);

        if(responseAnalyzer != null){
            if(responseAnalyzer.getTimeOutStatus()!=1) {
                responseAnalyzer.showPacket(args[0]);
            }
            else{
                responseAnalyzer.showTimeoutMessage(args[0]);
            }
        }
    }
}
