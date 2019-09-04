import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class store{

	//instances for store
	private static ServerSocket serverSocket;
	private static Socket client;
	private static ArrayList<String> strings = new ArrayList<>();
	public static HashMap<Integer,Integer> prices = new HashMap<>();
	private static int input;
	private static char[] inputs = new char[5000];
	private static int counter;
	private static int endcounter;
	private static int start;
	private static int end;
	private static int earlierChar=-1;
	private static InputStream in;
	private static OutputStream out;
	private static InputStreamReader reader;
	private static BufferedReader bufferedReader;
	private static String responseGetIndexHtml = "HTTP/1.1 200 OK\r\n\r\n";
	private static int lineNumber;
	private static int characterNumber;
	private static int length;
	private static int itemNo;
	private static String name;
	private static String familyName;
	private static int postCode;
	private static int creditCard;
	private static int quantity;
	private static int BANK_PORT;
	private static String BANK_IP;
	private static int STORE_PORT;
	
	public static void main(String[] args) throws Exception{
		//getting port and ip information from args
		STORE_PORT = Integer.parseInt(args[0]);
		BANK_IP = args[1];
		BANK_PORT = Integer.parseInt(args[2]);
		serverSocket = new ServerSocket(STORE_PORT);
		make_price_list();
		
		while(true) {
			//waiting for clients
			client = serverSocket.accept();
			in = client.getInputStream();
			out = client.getOutputStream();
			reader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(reader);
			//initializing variables
			counter = 0;
			endcounter = 0;
			//reading input
			while((input = bufferedReader.read())!=-1){
				char x = (char) input;
				inputs[counter++] = x;
				System.out.print(x);
				//differentiating between GET and POST method
				if(inputs[0] == 'G'){
					//checking for carry return and newline
					if(earlierChar == 13 && input == 10){
						endcounter++;
						earlierChar = input;
						//successive carry return and newline end of input
						if(endcounter == 2){
							operate();
							client = serverSocket.accept();
							in = client.getInputStream();
							out = client.getOutputStream();
							reader = new InputStreamReader(in);
							bufferedReader = new BufferedReader(reader);
						}
					}
					else if(earlierChar == 10 && input == 13){
						earlierChar = input;
					}
					else{
						endcounter=0;
						earlierChar = input;
					}
				}
				//POST method
				else{
					if(earlierChar == 13 && input == 10){
						endcounter++;
						lineNumber++;
						earlierChar = input;
						if(endcounter == 2){
							preparePostMessage();
							client = serverSocket.accept();
							in = client.getInputStream();
							out = client.getOutputStream();
							reader = new InputStreamReader(in);
							bufferedReader = new BufferedReader(reader);
						}
					}
					else if(earlierChar == 10 && input == 13){
						earlierChar = input;
					}
					else{
						endcounter=0;
						earlierChar = input;
						if(lineNumber == 8) {
							if(input!=13 && input!=10){
								characterNumber++;
								if(characterNumber>=17){
									length=length*10+input-48;
								}
							}
						}
					}
				}
			}
			//closing client
			client.close();
		}
	}
	//response to GET method
	private static void operate() throws Exception{
		String string = new String(inputs,0,counter);
		String[] strings = string.split("\n");
		
		String[] stringParts = strings[0].split(" ");
		if(stringParts[0].equals("GET")){
			//send index.html file data
			if(stringParts[1].equals("/index.html")){
				String writeString = "";
				File file = new File("index.html");
				Scanner scanner = new Scanner(file);
				while(scanner.hasNextLine()){
					writeString = writeString + scanner.nextLine() + "\n";
				}
				out.write(responseGetIndexHtml.getBytes());
				out.write(writeString.getBytes());
				client.close();
				counter=0;
				endcounter = 0;
				earlierChar = -1;
			}
			//ignoring favicon.ico request
			else if(stringParts[1].equals("/favicon.ico")){
				client.close();
				counter=0;
				endcounter = 0;
				earlierChar = -1;
			}
		}
		else{
			client.close();
			counter=0;
			endcounter = 0;
			earlierChar = -1;
		}
 	}
 	
 	private static void preparePostMessage() throws Exception{
 		String postMessage = "";
 		for(start=0;start<length;start++){
 			postMessage+=(char)bufferedReader.read();
 		}
 		System.out.println(postMessage);
 		length=0;
 		characterNumber=0;
 		lineNumber=0;
 		counter=0;
		endcounter = 0;
		earlierChar = -1;
		String[] strings = postMessage.split("\\&");
		String[] values = strings[0].split("\\=");
		itemNo = Integer.parseInt(values[1]);
		values = strings[1].split("\\=");
		quantity = Integer.parseInt(values[1]);
		values = strings[2].split("\\=");
		name = values[1];
		values = strings[3].split("\\=");
		familyName = values[1];
		values = strings[4].split("\\=");
		postCode = Integer.parseInt(values[1]);
		values = strings[5].split("\\=");
		creditCard = Integer.parseInt(values[1]);
		int result = getInfoFromBank(name, familyName, postCode, creditCard, prices.get(itemNo)*quantity);
		//sending html message according to the response from bank
		if(result == 0){
			String strs = "HTTP/1.1 200 OK\r\n";
	                strs+="Content-Length: 39\r\n";
        	        strs+="Content-Type: text/html\r\n";
			strs+="Connection: keep-alive\r\n";
                	strs+="\r\n";
                	strs+="The user information entered is invalid";
                	out.write(strs.getBytes());
		}
		else if(result == 1){
			String strs = "HTTP/1.1 200 OK\r\n";
	                strs+="Content-Length: 20\r\n";
        	        strs+="Content-Type: text/html\r\n";
			strs+="Connection: keep-alive\r\n";
                	strs+="\r\n";
                	strs+="Transaction Approved";
                	out.write(strs.getBytes());
		}
		else {
			String strs = "HTTP/1.1 200 OK\r\n";
	                strs+="Content-Length: 100\r\n";
        	        strs+="Content-Type: text/html\r\n";
			strs+="Connection: keep-alive\r\n";
                	strs+="\r\n";
                	strs+="Your account does not have sufficient credit for the requested transaction";
                	out.write(strs.getBytes());
		}
		client.close();
 	}
 	
 	private static int getInfoFromBank(String name, String familyName, int postCode, int creditCard, int credit) throws Exception{
		 Socket bank = new Socket(BANK_IP,BANK_PORT);
 		DataInputStream dataInputStream = new DataInputStream(bank.getInputStream());
 		DataOutputStream dataOutputStream = new DataOutputStream(bank.getOutputStream());
 		String input = dataInputStream.readUTF();
 		if(input.equals("ready")){
 			input = "";
			 String output = name+" "+familyName+" "+Integer.toString(postCode)+" "+Integer.toString(creditCard);
			 System.out.println(output);
 			dataOutputStream.writeUTF(output);
 			input = dataInputStream.readUTF();
 			System.out.println(input);
 			if(input.equals("User found")){
 				input = "";
 				output = Integer.toString(credit);
 				dataOutputStream.writeUTF(output);
 				input = dataInputStream.readUTF();
 				System.out.println(input);
 				if(input.equals("Transaction Failed")) return 2;
 				else return 1;
 			}
		 }
		 return 0;
	 }
	 
	 private static void make_price_list() {
		prices.put(1,200);
		prices.put(2,200);
		prices.put(3,200);
		prices.put(4,200);
		prices.put(5,200);
		prices.put(6,200);
		prices.put(7,120);
		prices.put(8,120);
		prices.put(9,120);
		prices.put(10,120);
		prices.put(11,120);
		prices.put(12,120);
		prices.put(13,120);
		prices.put(14,120);
		prices.put(15,120);
		prices.put(16,120);
	 }
}
