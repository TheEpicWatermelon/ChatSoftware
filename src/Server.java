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
    private static final int COMMAND_LEN = 3;
    private static final String COMMAND_QUIT = "svt";
    private static final String COMMAND_NEW = "svn";
    private static final String COMMAND_MSG = "msg";
    private static final String NEW_USER_COMMAND = "cnu";
    private static final String PRIVATE_MSG_COMMAND = "pmg";
    private static final String USER_LEAVE_MSG = "cul";
    private static Gui gui;
    private volatile static String inputText;

    /** Main
     * @param args parameters from command line
     */
    public static void main(String[] args) throws IOException{
        gui = new Gui();
        Thread console = new Thread(new consoleThread());
        console.start();
        new Server().go(); //start the server
    }

    /** Go
     * Starts the server
     */
    public void go() {
        System.out.println("Waiting for a client connection..");

        Socket client = null;//hold the client connection

        try {
            serverSock = new ServerSocket(5000);  //assigns an port to the server
            //serverSock.setSoTimeout(25000);  //5 second timeout
            while(running) {  //this loops to accept multiple clients
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
                gui.appendToConsole("Client Connected");
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
                            user.setListNum(connectionHandlers.size()+1);
                            giveUsers();
                        } else if (userInput.startsWith(COMMAND_MSG)){
                            String msg = userInput.substring(COMMAND_LEN);
                            int nIndex = userInput.indexOf('n');
                            // get the receiver's user number
                            int receiverNum = Integer.parseInt(msg.substring(0,nIndex));
                            msg.substring(nIndex+1);
                            writeToUsers(msg, receiverNum, user.getListNum());

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


            // send to all users that the user left
            sendUserLeft(user.getListNum());
            //Send a message to the client
            users.remove(user);

            close();
        } // end of run()

        private void sendUserLeft(int listNum) {
            String out = USER_LEAVE_MSG + listNum;
            writeToUsers(out,0,listNum);
        }

        private void giveUsers() {
            StringBuilder string = new StringBuilder();

            string.append(NEW_USER_COMMAND);
            for (int i = 0; i < connectionHandlers.size(); i++) {
                string.append(connectionHandlers.get(i).user.getName());
                if (i != connectionHandlers.size()-1) {
                    string.append(',');
                }
            }
            writeToUsers(string.toString(), 0,user.getListNum());
        }

        private void writeToUsers(String msg, int receiver, int sender){
            if (receiver == 0){
                String out = COMMAND_MSG + sender + 'n' + msg;
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    connectionHandlers.get(i).write(out);
                }
            }else {
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    String out = PRIVATE_MSG_COMMAND + sender + 'n' + msg;
                    if (connectionHandlers.get(i).user.getListNum() == sender || connectionHandlers.get(i).user.getListNum() == receiver) {
                        connectionHandlers.get(i).write(msg);
                    }
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


    private static class consoleThread implements Runnable {
        volatile String serverIn;

        @Override
        public void run() {
            while (true) {
                serverIn = inputText;
                if (serverIn == null){
                    continue;
                }
                if (serverIn.equals("close")) {
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        connectionHandlers.get(i).write("~~SERVER CLOSING IN 20 SECONDS~~");
                        gui.appendToConsole("Shutting down server...");
                    }
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        connectionHandlers.get(i).close();
                    }

                    try {
                        serverSock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                        System.exit(1);
                    }
                }
            }
        }
    }

} //end of Server class