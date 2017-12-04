/**
 * [Server.java]
 */

//imports for network communication

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Server {

    static ServerSocket serverSock;// server socket for connection
    static boolean running = true;  // controls if the server is accepting clients
    static List<User> users = Collections.synchronizedList(new ArrayList<>()); // syncronized list of users
    static List<ConnectionHandler> connectionHandlers = Collections.synchronizedList(new ArrayList<>());
    private static final int COMMAND_LEN = 4;
    private static final String COMMAND_QUIT = "svt";
    private static final String COMMAND_NEW = "svn";
    private static final String COMMAND_CHANNEL = "svc";
    private static final String COMMAND_MSG = "msg";

    /** Main
     * @param args parameters from command line
     */
    public static void main(String[] args) throws IOException{
        new Server().go(); //start the server
        BufferedReader serverInput = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            String serverIn = serverInput.readLine();
            if (serverIn.equals("close")){
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    connectionHandlers.get(i).write("~~SERVER CLOSING IN 20 SECONDS~~");
                }
                try {
                    Thread.sleep(20000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                    System.exit(1);
                }
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    connectionHandlers.get(i).close();
                }

                serverSock.close();
                System.exit(1);
            }
        }
    }

    /** Go
     * Starts the server
     */
    public void go() {
        System.out.println("Waiting for a client connection..");

        Socket client = null;//hold the client connection

        try {
            serverSock = new ServerSocket(5000);  //assigns an port to the server
            serverSock.setSoTimeout(5000);  //5 second timeout
            while(running) {  //this loops to accept multiple clients
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
                //Note: you might want to keep references to all clients if you plan to broadcast messages
                //Also: Queues are good tools to buffer incoming/outgoing messages
                User user = new User();
                users.add(user);
                ConnectionHandler connect = new ConnectionHandler(client,user);
                connectionHandlers.add(connect);
                Thread t = new Thread(connect); //create a thread for the new client and pass in the socket
                t.start(); //start the new thread
            }
        }catch(Exception e) {
            // System.out.println("Error accepting connection");
            //close all and quit
            try {
                client.close();
            }catch (Exception e1) {
                System.out.println("Failed to close socket");
            }
            System.exit(-1);
        }
    }

    //***** Inner class - thread for client connection
    class ConnectionHandler implements Runnable {
        private PrintWriter output; //assign printwriter to network stream
        private BufferedReader input; //Stream for network input
        private Socket client;  //keeps track of the client socket
        private boolean running;
        private final User user;
        /* ConnectionHandler
         * Constructor
         * @param the socket belonging to this client connection
         */
        ConnectionHandler(Socket s, User user) {
            this.user = user;
            this.client = s;  //constructor assigns client to this
            try {  //assign all connections to client
                this.output = new PrintWriter(client.getOutputStream());
                InputStreamReader stream = new InputStreamReader(client.getInputStream());
                this.input = new BufferedReader(stream);
            }catch(IOException e) {
                e.printStackTrace();
            }
            running=true;
        } //end of constructor


        /* run
         * executed on start of thread
         */
        public void run() {

            //Get a message from the client
            String userInput="";

            //Get a message from the client
            while(running) {  // loop unit a message is received
                try {
                    if (this.input.ready()) { //check for an incoming messge
                        userInput = this.input.readLine();  //get a message from the client

                        System.out.println("msg from client: " + userInput);

                        if (userInput.startsWith(COMMAND_QUIT)) {
                            running = false; //stop receving messages
                        } else if(userInput.startsWith(COMMAND_NEW)){
                            user.setName(userInput.substring(COMMAND_LEN));
                        } else if (userInput.startsWith(COMMAND_CHANNEL)){
                            user.setChannel(Integer.parseInt(userInput.substring(COMMAND_LEN)));
                        } else if (userInput.startsWith(COMMAND_MSG)){
                            String msg = userInput.substring(COMMAND_LEN);
                            writeToUsers(msg, user.getChannel());
                        }else{
                            output.println("err Command Not Available");
                            output.flush();
                        }

                    }
                }catch (IOException e) {
                    System.out.println("Failed to receive userInput from the client");
                    e.printStackTrace();
                }
            }

            //Send a message to the client
            users.remove(user);

            close();
        } // end of run()

        private void writeToUsers(String msg, int channel){
            for (int i = 0; i < connectionHandlers.size(); i++) {
                if (connectionHandlers.get(i).user.getChannel() == channel){
                    connectionHandlers.get(i).write(msg);
                }
            }
        }

        private void write(String msg){
            output.println(msg);
            output.flush();
        }

        private void close(){
            //close the socket
            try {
                this.input.close();
                output.close();
                client.close();
            }catch (Exception e) {
                System.out.println("Failed to close socket");
            }
        }
    } //end of inner class
} //end of Server class