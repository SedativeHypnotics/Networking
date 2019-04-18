#include<bits/stdc++.h>
#include<sys/socket.h>
#include<netinet/in.h>
#include<sys/types.h>
#include<unistd.h>
#include<netdb.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>
using namespace std;

void showError(const char *message){
    perror(message); //perror produces a message for the last error encountered during a call to a system
    exit(1); //exits program with an unsuccessful status
}

bool isUser(char user[]){
    if (FILE *file = fopen(user, "r")) {
            fclose(file);
            return true;
    }
    return false;
}

inline int findPort(){
    int socketFileDescriptor; //file descriptor
    int port = -1; //assigning threshold value for port
    sockaddr_in serverAddress;
    hostent *hostAddress; //IP address of the host

    for(int i=1024;i<=USHRT_MAX;i++){
        socketFileDescriptor = socket(AF_INET,SOCK_STREAM,0);
        if(socketFileDescriptor<0) continue;
        bzero(&serverAddress,sizeof(&serverAddress));
        serverAddress.sin_family = AF_INET;
        serverAddress.sin_port = htons(i);

        hostAddress = gethostbyname("localhost");
        memcpy(&serverAddress.sin_addr, hostAddress->h_addr, hostAddress->h_length);
        if(connect(socketFileDescriptor,(sockaddr *)&serverAddress,sizeof(serverAddress)) < 0){
            port = i;
            break;
        }
    }
    close(socketFileDescriptor);
    return port;
}

int main(int argc, char *argv[]){
	sockaddr_in serverAddress, clientAddress; //this contains the internet address of the server and client
    int clientFileDescriptor, socketFileDescriptor; //these file descriptors store the values returned by the socket system call and the accept system calls
    int serverPort; //the port number of the server for accepting connection requests
    int number; //number of characters read or write
    socklen_t clientAddressSize,serverAddressSize; //size of the client address
    char inputBuffer[4096]; //stores the data from client in this string

    bzero((char *) &serverAddress, sizeof(serverAddress)); //clearing the serverAddress struct to avoid garbage values
    memset(inputBuffer, 0, sizeof(inputBuffer)); //clearing the input buffer to avoid garbage values

    socketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0); // socket parameters 1) domain and it is AF_INET (IPv4 protocol)
                                                            //					 2) communication type, SOCK_STREAM denotes a TCP connection which is reliable, connection oriented
    														//					 3) protocol value, that means the OS will select the appropiate protocol
    														// //socket() function returns an integer denoting the reference of the created socket

    if(socketFileDescriptor < 0){
    	showError("Failed opening socket"); //failure to create a socket will return -1
    }
    serverPort = findPort(); //assigning listening port number to the integer type serverPort
    //printf("%d\n", argv[1]);

    serverAddress.sin_family = AF_INET; //the address family. AF_INET denotes IPv4
    serverAddress.sin_port = htons(serverPort); //the port number. converting the port number to network byte order using htons function
    serverAddress.sin_addr.s_addr = INADDR_ANY; //sets the ip address of the device

    int result = bind(socketFileDescriptor, (sockaddr *) &serverAddress, sizeof(serverAddress)); //binding the serverAddress to the file descriptor
    if(result < 0) showError("Failed to create server!\n");

    serverAddressSize = sizeof(serverAddress);
    printf("Mail Server started\nListening to port: %d\n",serverPort);

    //smtp replies
    char welcomeMessage[] = "220 Welcome to mailServer05\n";
    char replyDenied[] = "421 MAIL Service not available, closing transmission channel\n";
    char replyHeloOkay[] = "250 Requested mail action okay, completed\n";
    char replyHeloError[] = "500 Syntax error, command unrecognised\n";
    char replyHeloError2[] = "501 Syntax error in parameters or arguments\n";
    char replyMailOkay[] = "250 Requested mail action okay, completed\n";
    char replyMailError[] = "500 Syntax error, command unrecognised\n";
    char replyMailError2[] = "501 Syntax error in parameters or arguments\n";
    char replyRcptOkay[] = "250 Requested mail action okay, completed\n";
    char replyRcptError[] = "450 Requested mail action not taken: mailbox unavailable\n";
    char replyRcptErrorSyntax[] = "500 Syntax error, command unrecognised\n";
    char replyRcptErrorSyntax2[] = "501 Syntax error in parameters or arguments\n";
    char replyData[] = "354 Start mail input; end with <CRLF>.<CRLF>\n";
    char replyDataError[] = "500 Syntax error, command unrecognised\n";
    char replyReceivedData[] = "250 Requested mail action okay, completed\n";
    char replyQuit[] = "221 Service closing transmission channel\n";
    char replyQuitError[] = "500 Syntax error, command unrecognised\n";

    while(1){
    	listen(socketFileDescriptor,4); //listens for connections. the second argument denotes the number of connections that can be waiting while the process is handling a particular connection
        clientAddressSize = sizeof(clientAddress); // size of the client address
    	clientFileDescriptor = accept(socketFileDescriptor, (sockaddr *) &clientAddress, &clientAddressSize); //accept unction call returns a file descriptor for client

    	if(clientFileDescriptor == -1) showError("Couldn't accept mailClient"); //error accepting
        //sending welcoming message
        number = write(clientFileDescriptor,welcomeMessage,sizeof(welcomeMessage)/sizeof(char));
    	if(number < 0) showError("could not write!\n"); //message was not written successfully
        printf("SERVER: %s",welcomeMessage);
        bzero(inputBuffer,sizeof(inputBuffer));

        //reading HELO from client
    	number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
    	if(number < 0) showError("could not read!\n"); //message was not read successfully
        printf("CLIENT: %s\n",inputBuffer);

    	//sending reply to HELO
        if(inputBuffer[0]!='H'||inputBuffer[1]!='E'||inputBuffer[2]!='L'||inputBuffer[3]!='O'){
            //sending error reply to client
            number = write(clientFileDescriptor,replyHeloError,sizeof(replyHeloError)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyHeloError);
            continue;
        } else{
            number = write(clientFileDescriptor,replyHeloOkay,sizeof(replyHeloOkay)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyHeloOkay);
        }
        bzero(inputBuffer,sizeof(inputBuffer));

        //recieving MAIL FROM message from client
        number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
    	if(number < 0) showError("could not read!\n"); //message was not read successfully
        printf("CLIENT: %s\n",inputBuffer);

        //sending reply to MAIL FROM
        if(inputBuffer[0]!='M'||inputBuffer[1]!='A'||inputBuffer[2]!='I'||inputBuffer[3]!='L'
           ||inputBuffer[4]!=' '||inputBuffer[5]!='F'||inputBuffer[6]!='R'||inputBuffer[7]!='O'
           ||inputBuffer[8]!='M'){
            //sending error reply to client
            number = write(clientFileDescriptor,replyMailError,sizeof(replyMailError)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyMailError);
            continue;
        } else{
	    //creating empty file
	    char clientName[300];
	    sscanf(inputBuffer,"%s %s %[^@]",clientName,clientName,clientName);
	    strcat(clientName,".txt");
	    FILE *clientFile = fopen(clientName,"w");
	    fclose(clientFile);

	    //sending reply to client
            number = write(clientFileDescriptor,replyMailOkay,sizeof(replyMailOkay)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyMailOkay);
        }
        bzero(inputBuffer,sizeof(inputBuffer));

        //Receiving RCPT TO request
        number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
    	if(number < 0) showError("could not read!\n"); //message was not read successfully
        printf("CLIENT: %s\n",inputBuffer);

        //sending RCPT TO response
        char token1[100],token2[100],user[100],host[100];
        sscanf(inputBuffer,"%s %s %[^@]@%s",token1,token2,user,host);
        if(strcmp(token1,"RCPT")==0&&strcmp(token2,"TO")==0){
            strcat(user,".txt");
            if(isUser(user)){
                number = write(clientFileDescriptor,replyRcptOkay,sizeof(replyRcptOkay)/sizeof(char));
                if(number < 0) showError("could not write!\n"); //message was not written successfully
                printf("SERVER: %s",replyRcptOkay);
            } else{
                number = write(clientFileDescriptor,replyRcptError,sizeof(replyRcptError)/sizeof(char));
                if(number < 0) showError("could not write!\n"); //message was not written successfully
                printf("SERVER: %s",replyRcptError);
                continue;
            }
        } else{
            number = write(clientFileDescriptor,replyRcptErrorSyntax,sizeof(replyRcptErrorSyntax)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyRcptError);
            continue;
        }
        bzero(inputBuffer,sizeof(inputBuffer));

        //receiving DATA request
        number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
    	if(number < 0) showError("could not read!\n"); //message was not read successfully
        printf("CLIENT: %s\n",inputBuffer);

        //sending DATA reply
        if(strcmp(inputBuffer,"DATA")==0){
            number = write(clientFileDescriptor,replyData,sizeof(replyData)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyData);
        }
        else{
            number = write(clientFileDescriptor,replyDataError,sizeof(replyDataError)/sizeof(char));
            if(number < 0) showError("could not write!\n"); //message was not written successfully
            printf("SERVER: %s",replyDataError);
            continue;
        }
        bzero(inputBuffer,sizeof(inputBuffer));

        //receiving data stream
        FILE* file = fopen(user,"a");
        number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
        if(number < 0) showError("could not read!\n"); //message was not read successfully
        fprintf(file,"%s",inputBuffer);
        bzero(inputBuffer,sizeof(inputBuffer));

        number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
        if(number < 0) showError("could not read!\n"); //message was not read successfully
        fprintf(file,"%s",inputBuffer);
        bzero(inputBuffer,sizeof(inputBuffer));

        while(1){
            number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
            if(number < 0) showError("could not read!\n"); //message was not read successfully
            if(inputBuffer[0]=='.'){
                //printf("---> 1\n");
                break;
            }
            if(number==0){
                //printf("---> 2\n");
                break;
            }
            if(inputBuffer[0]==10 &&strlen(inputBuffer)==1){
                //printf("---> 3\n");
                continue;
            }
            fprintf(file,"%s",inputBuffer);
            //printf("%d: %d %s",number,inputBuffer[0],inputBuffer);
            bzero(inputBuffer,sizeof(inputBuffer));
            number=0;
        }
        bzero(inputBuffer,sizeof(inputBuffer));
        fprintf(file,"\n");
        //receiving quit message
        number = read(clientFileDescriptor, inputBuffer, 1023); //read the message from client
    	if(number < 0) showError("could not read!\n"); //message was not read successfully
        printf("CLIENT: %s\n",inputBuffer);
        bzero(inputBuffer,sizeof(inputBuffer));

        fclose(file);

	char clientIp[INET_ADDRSTRLEN];
	inet_ntop(AF_INET, &(clientAddress.sin_addr), clientIp, INET_ADDRSTRLEN);
	int clientPort = ntohs(clientAddress.sin_port); //storing client host number and ip

    	number = write(clientFileDescriptor,replyQuit,sizeof(replyQuit)/sizeof(char));
        if(number < 0) showError("could not write!\n"); //message was not written successfully
        printf("SERVER: %s",replyQuit);

	printf("Connection closed with client %s:%d\n",clientIp,clientPort);
	printf("\n");
    }
    return 0;
}
/*
References:
    1) http://www.linuxhowtos.org/C_C++/socket.htm
    2) http://www.rfc-editor.org/rfc/rfc2821.txt
    3) http://www.greenend.org.uk/rjk/tech/smtpreplies.html
    4) http://www.stackoverflow.com
*/
