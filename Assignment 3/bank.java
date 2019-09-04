import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.StringTokenizer;

public class bank {
    //instances for dealing with user data
    private static ArrayList<User> users = new ArrayList<>();
    private static User validUser;

    public static void main(String[] args) throws Exception {
	// getting port number from argument
        int port = Integer.parseInt(args[0]);
        //creating a server socket
        ServerSocket serverSocket = new ServerSocket(port);
        getUsers();
        while (true){
            //waiting for connection
            Socket client = serverSocket.accept();
            DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
            //calling client for sending user data
            dataOutputStream.writeUTF("ready");
            //getting user data from client
            String input = dataInputStream.readUTF();
            System.out.println("Server connected with port: "+client.getPort());
            //getting individual data field values from user input
            StringTokenizer stringTokenizer = new StringTokenizer(input," ");
            String name = stringTokenizer.nextToken();
            String familyName = stringTokenizer.nextToken();
            int postCode = Integer.parseInt(stringTokenizer.nextToken());
            int creditCard = Integer.parseInt(stringTokenizer.nextToken());
            User clientUser = new User(name,familyName,postCode,creditCard);
            System.out.println(clientUser.toString());
            if(findUser(clientUser)){
          	//searching if the user is valid
                System.out.println("User found");
                //calling on client that the user data is valid
                dataOutputStream.writeUTF("User found");
                //getting credit value from client
                int data = Integer.parseInt(dataInputStream.readUTF());
                //handling purchase operation
                boolean valid = handlePurchase(data);
                if(valid){
               	    //purchase is successful. sending success message to client
                    dataOutputStream.writeUTF("Transaction Successful");
                    validUser.setCredit(validUser.getCredit()-data);
                    validUser.setBalance(validUser.getBalance()+data);
                    writeToDatabase();
                }
                else{
                    //purchase ha failed due to insufficient credit. sending failed message
			System.out.println("Transaction Failed");
                    dataOutputStream.writeUTF("Transaction Failed");
                }
            }
            //user data not valid
            else{
                System.out.println("User not found");
                dataOutputStream.writeUTF("User not found");
            }
            System.out.println();
            client.close();
        }
    }

    //writing on database.txt
    private static void writeToDatabase() throws FileNotFoundException {
        for (User user : users){
            if(user.equals(validUser)){
                user = validUser;
                break;
            }
        }
        File file = new File(System.getProperty("user.dir")+"/database.txt");
        PrintStream printStream = new PrintStream(file);
        for (User user : users){
            printStream.println(user.toString());
        }
        printStream.close();
    }

    private static boolean handlePurchase(int credit) {
        if(validUser.getCredit()>=credit) return true;
        return false;
    }

    private static void getUsers() throws Exception {
        File file = new File(System.getProperty("user.dir")+"/database.txt");
        Scanner scanner = new Scanner(file);
        while(scanner.hasNext()){
            String name = scanner.next();
            String familyName = scanner.next();
            int postCode = scanner.nextInt();
            int creditCard = scanner.nextInt();
            int balance = scanner.nextInt();
            int credit = scanner.nextInt();
            users.add(new User(name,familyName,postCode,creditCard,balance,credit));
        }
        scanner.close();
    }

    private static boolean findUser(User user){
        for(User user1 : users){
            if(user1.equals(user)){
                validUser = user1;
                return true;
            }
        }
        return false;
    }

    //user data class
    private static class User{
        private String name;
        private String familyName;
        private int postCode;
        private int creditCard;
        private int balance;
        private int credit;

        User(String name, String familyName, int postCode, int creditCard) {
            this.name = name;
            this.familyName = familyName;
            this.postCode = postCode;
            this.creditCard = creditCard;
        }

        public User(String name, String familyName, int postCode, int creditCard, int balance, int credit) {
            this.name = name;
            this.familyName = familyName;
            this.postCode = postCode;
            this.creditCard = creditCard;
            this.balance = balance;
            this.credit = credit;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public int getPostCode() {
            return postCode;
        }

        public void setPostCode(int postCode) {
            this.postCode = postCode;
        }

        public int getCreditCard() {
            return creditCard;
        }

        public void setCreditCard(int creditCard) {
            this.creditCard = creditCard;
        }

        public int getBalance() {
            return balance;
        }

        public void setBalance(int balance) {
            this.balance = balance;
        }

        public int getCredit() {
            return credit;
        }

        public void setCredit(int credit) {
            this.credit = credit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return postCode == user.postCode &&
                    creditCard == user.creditCard &&
                    name.equals(user.name) &&
                    familyName.equals(user.familyName);
        }

        @Override
        public String toString() {
            return  name + " "
                    + familyName + " "
                    + postCode + " "
                    + creditCard + " "
                    + balance + " "
                    + credit + " ";
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, familyName, postCode, creditCard, balance, credit);
        }
    }
}
