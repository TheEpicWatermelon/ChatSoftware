/**
 * [Server.java]
 */

//imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Server {// open main class

    //global variables
    private static ServerSocket serverSock;// server socket for connection
    private static boolean running = true;  // controls if the server is accepting clients
    private static List<User> users = Collections.synchronizedList(new ArrayList<>()); // synchronized list of users
    private static List<ConnectionHandler> connectionHandlers = Collections.synchronizedList(new ArrayList<>());// list of all the users connections
    private static final int COMMAND_LEN = 3;// length of the command - default length is 3
    private static final String COMMAND_QUIT = "svt"; // command for user disconnect
    private static final String COMMAND_NEW = "svn";// command used when user first joins the server
    private static final String COMMAND_MSG = "msg"; // command when user wants to send a msg to another user
    private static final String NEW_USER_COMMAND = "cnu"; // command that will be sent to all users stating that there is a new user connected
    private static final String PRIVATE_MSG_COMMAND = "pmg"; // private message command
    private static final String USER_LEAVE_MSG = "cul"; // command that will be sent to users when a user leaves
    private static Gui gui; // GUI
    volatile static String serverIn; // string that will hold the console input

    /** Main
     * stars gui and server
     * @param args parameters from command line
     */
    public static void main(String[] args) throws IOException{
        gui = new Gui();
        Thread console = new Thread(new consoleThread());
        console.start();
        new Server().go(); //start the server
    }

    /** Go
     * Starts the server and accepts users
     */
    public void go() {
        System.out.println("Waiting for a client connection..");

        Socket client = null;//hold the client connection

        try {
            serverSock = new ServerSocket(5000);  //assigns an port to the server
            //serverSock.setSoTimeout(25000);  //5 second timeout
            while(running) {  //loops to accepts client
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
                gui.appendToConsole("Client Connected");
                // create a new user
                User user = new User();
                users.add(user); // add user to the list
                // create a connection handler for the user
                ConnectionHandler connect = new ConnectionHandler(client,user);
                connectionHandlers.add(connect);
                Thread t = new Thread(connect); //create a thread for the new client and pass in the connection handler
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

    /**
     * ConnectionHandler
     * handles a users connection to the server and handles their inputs
     */
    class ConnectionHandler implements Runnable {
        private PrintWriter output; //assign printwriter to network stream
        private BufferedReader input; //Stream for network input
        private Socket client;  //keeps track of the client socket
        private boolean running; // boolean that will be true when client is not quitting
        private final User user; // holds the user for this connection
        /* ConnectionHandler
         * Constructor
         * @param the socket belonging to this client connection
         */
        ConnectionHandler(Socket s, User user) {
            this.user = user;// set yser
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
         * it will handle the user inputs and will close when user leaves
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

            updateUserRemoved();

            close();
        } // end of run()

        private void updateUserRemoved() {
            users.remove(user);

            // update the remaining users list numbers
            User.updateNums(users);
        }

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
        @Override
        public void run() {
            while (true) {
                if (serverIn == null){
                    continue;
                }
                if (serverIn.equals("close")) {
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        connectionHandlers.get(i).write("~~SERVER CLOSING IN 20 SECONDS~~");
                    }
                    gui.appendToConsole("Shutting down server...");
                    System.out.println("Shutting down");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    System.out.println("Waiting Done");
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        connectionHandlers.get(i).close();
                    }

                    try {
                        serverSock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    System.out.println("Shutting down");
                    System.exit(1);
                }else if (serverIn.startsWith("kick")){
                    try {
                        serverIn = serverIn.substring(5);// substring kick and number after
                    }catch(StringIndexOutOfBoundsException e){
                        gui.appendToConsole("No user specified");
                        serverIn = null;
                        continue;
                    }
                    int kickNum;
                    try {
                        kickNum = Integer.parseInt(serverIn);
                    }catch (StringIndexOutOfBoundsException e){
                        gui.appendToConsole("No user specified");
                        serverIn = null;
                        continue;
                    }
                    ConnectionHandler user = null;
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        if (connectionHandlers.get(i).user.getListNum() == kickNum){
                            user = connectionHandlers.get(i);
                            break;
                        }
                    }
                    if (user == null){
                        gui.appendToConsole("This is not a valid user");
                        serverIn = null;
                        continue;
                    }
                    user.writeToUsers(user.user.getName() + " has been kicked from the chat", 0, kickNum);
                    user.updateUserRemoved();
                    user.write("You have been kicked");
                    user.close();
                    gui.appendToConsole("Kicked user number " + kickNum);
                    serverIn = null;
                }
            }
        }
    }

} //end of Server class