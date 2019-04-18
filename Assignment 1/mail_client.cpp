#include<bits/stdc++.h>
#include<sys/socket.h>
#include<netinet/in.h>
#include<sys/types.h>
#include<netdb.h>
#include<unistd.h>
#include<limits.h>
using namespace std;

void showError(const char *message) {
    perror(message); //perror produces a message for the last error encountered during a call to a system
    exit(1); //exits program with an unsuccessful status
}

int main(int argc, char* argv[]) {
    int socketFileDescriptor; //thisfile descriptor store the value returned by the socket system call and the accept system calls
    int serverPort; //the port number of the server for accepting connection requests
    int number; //number of characters read or write
    sockaddr_in serverAddress; //this contains the internet address of the server
    hostent *server; //this defines a host in the internet

    if(argc < 4) {
        printf("usage %s user@hostname:port \"subject\" input_file.txt\n", argv[0]);
        exit(0);
    }
    char localhost[HOST_NAME_MAX];  //getting local user data
    char loginname[300];

    gethostname(localhost,HOST_NAME_MAX);
    getlogin_r(loginname,LOGIN_NAME_MAX);


    strcat(loginname,"@");
    strcat(loginname,localhost); //constructing sender mail address

    char sendto[100],host[100],port[100],receiverAddress[100];
    sscanf(argv[1],"%[^@]@%[^:]:%s",sendto,host,port); //extracting receivername, hostname and port number as string

    //constructing receiver mail address
    strcpy(receiverAddress,sendto);
    strcat(receiverAddress,"@");
    strcat(receiverAddress,host);

    char *subject = argv[2];
    FILE* file = fopen(argv[3],"r"); //input file
    char inputBuffer[4096]; //stores the data from server in this string
    serverPort = atoi(port); //assigning listening port number to the integer type serverPort

    socketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0); // socket parameters 1) domain and it is AF_INET (IPv4 protocol)
    //					 2) communication type, SOCK_STREAM denotes a TCP connection which is reliable, connection oriented
    //					 3) protocol value, that means the OS will select the appropiate protocol
    // //socket() function returns an integer denoting the reference of the created socket

    if(socketFileDescriptor == -1) {
        showError("Failed opening socket"); //failure to create a socket will return -1
    }

    server = gethostbyname(host); //takes the name of the host and returns a pointer of that host

    if(server == NULL) {
        showError("No such host\n");
    }
    bzero((char *) &serverAddress, sizeof(serverAddress)); //clearing the serverAddress struct to avoid garbage values
    serverAddress.sin_family = AF_INET; //the address family. AF_INET denotes IPv4
    bcopy((char *)(*server).h_addr,
          (char *)&serverAddress.sin_addr.s_addr,
          (*server).h_length); // sets the fields in serverAddress
    serverAddress.sin_port = htons(serverPort);

    //smtp messages
    char HELO[] = "HELO";
    char MAILFROM[300];
    strcpy(MAILFROM,"MAIL FROM ");
    strcat(MAILFROM,loginname);
    string user = "RCPT TO ";
    user+=receiverAddress;
    char DATA[] = "DATA";
    char QUIT[] = "QUIT";

    bzero(inputBuffer,4095); //clearing the inputbuffer to avoid garbage values
    if (connect(socketFileDescriptor,(sockaddr *)&serverAddress,sizeof(serverAddress)) < 0) {
        showError("Error connecting to the server\n"); //The connect function is called by the client to establish a connection to the server
    }

    //reading welcome message from server
    number = read(socketFileDescriptor,inputBuffer,4095);
    if(number<0) {
        showError("message was not received\n");
    } else {
        int len = strlen(inputBuffer);
        for(int i= 4; i<len; i++) {
            printf("%c",inputBuffer[i]);
        }
        printf("\n");
    }

    //sending HELO to server
    number = write(socketFileDescriptor,HELO,strlen(HELO));
    if(number<0) {
        showError("message was not sent\n");
    }
    bzero(inputBuffer,4095); //clearing the inputbuffer to avoid garbage values

    //reading HELO reply
    number = read(socketFileDescriptor,inputBuffer,4095);
    if (number < 0) {
        showError("ERROR reading from socket");
    } else {
        if(inputBuffer[0] != '2') {
            printf("HELO: %s\n",inputBuffer);
            close(socketFileDescriptor);
            exit(0);
        }
    }

    //sending MAIL FROM to server
    number = write(socketFileDescriptor,MAILFROM,strlen(MAILFROM));
    if(number<0) {
        showError("message was not sent\n");
    }
    bzero(inputBuffer,4095); //clearing the inputbuffer to avoid garbage values

    //reading MAIL FROM reply
    number = read(socketFileDescriptor,inputBuffer,4095);
    if (number < 0) {
        showError("ERROR reading from socket");
    } else {
        if(inputBuffer[0] != '2') {
            printf("MAIL FROM: %s\n",inputBuffer);
            close(socketFileDescriptor);
            exit(0);
        }
    }
    //printf("--------------\n");
    //sending rcpt to
    number = write(socketFileDescriptor,user.c_str(),strlen(user.c_str()));
    if(number<0) {
        showError("message was not sent\n");
    }
    bzero(inputBuffer,4095); //clearing the inputbuffer to avoid garbage values

    //reply from rcpt to
    number = read(socketFileDescriptor,inputBuffer,4095);
    if (number < 0) {
        showError("ERROR reading from socket");
    } else {
        if(inputBuffer[0] != '2') {
            printf("RCPT TO: %s\n",inputBuffer);
            close(socketFileDescriptor);
            exit(0);
        }
    }
    bzero(inputBuffer,4095); //clearing the inputbuffer to avoid garbage values

    //sending DATA
    number = write(socketFileDescriptor,DATA,strlen(DATA));
    //printf("data sent\n");
    if(number<0) {
        showError("message was not sent\n");
    }

    //reply for DATA
    number = read(socketFileDescriptor,inputBuffer,4095);
    if (number < 0) {
        showError("ERROR reading from socket");
    } else {
        if(inputBuffer[0] != '2' && inputBuffer[0] != '3') {
            printf("DATA: %s\n",inputBuffer);
            close(socketFileDescriptor);
            exit(0);
        }
    }
    bzero(inputBuffer,4095); //clearing the inputbuffer to avoid garbage values

    //sending mail context

    //formatting FROM field
    char logindata[500];
    strcpy(logindata,"From: <");
    strcat(logindata,loginname);
    strcat(logindata,">\n");

    number = write(socketFileDescriptor,logindata,strlen(logindata));
    if(number<0) {
        showError("message was not sent\n");
    }

    //formatting TO field
    char tomessage[300];
    strcpy(tomessage,"To: <");
    strcat(tomessage,receiverAddress);
    strcat(tomessage,">\n");

    number = write(socketFileDescriptor,tomessage,strlen(tomessage));
    if(number<0) {
        showError("message was not sent\n");
    }

    char subjectmessage[300];

    strcpy(subjectmessage,"Subject: \"");
    strcat(subjectmessage,subject);
    strcat(subjectmessage,"\"\n");
    number = write(socketFileDescriptor,subjectmessage,strlen(subjectmessage));
    if(number<0) {
        showError("message was not sent\n");
    }

    //fetching current time from localhost
    time_t rawtime;
    struct tm * timeinfo;
    char buffer[80];

    time (&rawtime);
    timeinfo = localtime(&rawtime);

    strftime(buffer,sizeof(buffer),"%d-%m-%Y %A at %I:%M:%S %p",timeinfo);
    string str(buffer);

    char tme[20];
    strcpy(tme,"Sent: ");
    strcat(tme,str.c_str());
    strcat(tme,"\n");

    number = write(socketFileDescriptor,tme,strlen(tme));
    if(number<0) {
        showError("message was not sent\n");
    }

    usleep(200000);

    ///using intermediate buffer to resolve the data loss issue
    ///rather than sending single lines using fgets sending burst buffer
    ///minimizes the synchronization issue
    ///here interBuffer is a secondary buffer so to store the messages from the file
    ///then when the buffer exceeds a certain length
    ///the data will be sent using the socket
    bzero(inputBuffer,sizeof(inputBuffer));
    char interBuffer[2048];
    while(fgets(interBuffer,2047,file)) {
        //printf("%s\n",interBuffer);
        if(strlen(inputBuffer)>=2047){
            number = write(socketFileDescriptor,inputBuffer,strlen(inputBuffer));
            //printf("%s\n",inputBuffer);
            //printf("%d\n\n",inputBuffer[0]);
            if(number<0) {
                showError("message was not sent\n");
            }
            number = write(socketFileDescriptor,"\n",strlen("\n"));
            if(number<0) {
                showError("message was not sent\n");
            }
            bzero(inputBuffer,sizeof(inputBuffer));
            usleep(10000);
        }
        else{
            strcat(inputBuffer,interBuffer);
            bzero(interBuffer,sizeof(interBuffer));
        }
    }
    fclose(file);
    usleep(10000);
    if(strlen(inputBuffer)>0){
        number = write(socketFileDescriptor,inputBuffer,strlen(inputBuffer));
        //printf("%s\n",inputBuffer);
        //printf("%d\n\n",inputBuffer[0]);
        if(number<0) {
            showError("message was not sent\n");
        }
        number = write(socketFileDescriptor,"\n",strlen("\n"));
        if(number<0) {
            showError("message was not sent\n");
        }
    }
    usleep(1000);
    number = write(socketFileDescriptor,".",strlen("."));
    if(number<0) {
        showError("message was not sent\n");
    }
    bzero(inputBuffer,sizeof(inputBuffer));

    usleep(1000);
    number = write(socketFileDescriptor,QUIT,strlen(QUIT));
    if(number<0) {
        showError("message was not sent\n");
    }
    printf("Mail sent Successfully\n");
    ///printf("%s\n", inputBuffer);

    number = read(socketFileDescriptor,inputBuffer,4095);
    if (number < 0) {
        showError("ERROR reading from socket");
    } else {
        if(inputBuffer[0] != '2') {
            printf("QUIT: %s\n",inputBuffer);
            close(socketFileDescriptor);
            exit(0);
        }
    }

    close(socketFileDescriptor); //closing socket
    return 0;
}

/*
References:
    1) http://www.linuxhowtos.org/C_C++/socket.htm
    2) http://www.rfc-editor.org/rfc/rfc2821.txt
    3) http://www.greenend.org.uk/rjk/tech/smtpreplies.html
    4) http://www.stackoverflow.com
*/
