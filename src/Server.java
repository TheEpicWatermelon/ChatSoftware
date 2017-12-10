//imports
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [Server.java]
 * Runs a server on a port and accepts users that are able to chat between each other
 * @author Sasha Maximovitch
 * @date November 9th, 2017
 */

class Server {

    //global variables
    private static ServerSocket serverSock;// server socket for connection
    private static boolean running = true;  // controls if the server is accepting clients
    private static List<User> users = Collections.synchronizedList(new ArrayList<>()); // synchronized list of users
    private static List<ConnectionHandler> connectionHandlers = Collections.synchronizedList(new ArrayList<>());// list of all the users connections
    private static final int COMMAND_LEN = 3;// length of the command - default length is 3
    private static final String COMMAND_QUIT = "svt"; // command for user disconnect
    private static final String COMMAND_NEW = "svn";// command used when user first joins the server
    private static final String COMMAND_MSG = "msg"; // command when user wants to send a msg to another user
    private static final String UPDATE_USER_COMMAND = "upd"; // command that will be sent to all users stating that there is a new user connected
    private static final String PRIVATE_MSG_COMMAND = "pmg"; // private message command
    private static final String SERVER_MSG = "smg";
    private static final String KICK_COMMAND = "kck";
    private static final String PREVIOUS_CHAT = "pvc";
    private static Gui gui; // GUI
    volatile static String serverIn; // string that will hold the console input
    private static StringBuilder previousChat;

    /**
     * Main
     * starts gui, loads the previous chat and starts the server
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // initialize GUI
        gui = new Gui();
        // Start console input listener
        Thread console = new Thread(new consoleThread());
        console.start();
        // get chat history and add it into the previousChat String
        previousChat = new StringBuilder();
        previousChat.append(getPrevChat());
        // start the server
        new Server().go(); //start the server
    }

    /**
     * getPrevChat
     * Reads from the chat history text file and stores all the previous chat into a string
     * @return
     * @throws IOException
     */
    private static StringBuilder getPrevChat() throws IOException {
        StringBuilder prevChat = new StringBuilder();
        BufferedReader input = new BufferedReader(new FileReader("Chat History.txt"));
        String line = input.readLine();
        boolean first = true;
        while (line != null) {
            if (!line.isEmpty()) {
                if (first){
                    prevChat.append(line);
                    first = false;
                }else {
                    prevChat.append("\n" + line);
                }
            }
            line = input.readLine();
        }
        input.close();
        return prevChat;
    }

    /**
     * addChat
     * adds new chat to previous chat
     * @param msg
     */
    private static void addChat(String msg) {
        previousChat.append(msg);
    }

    /**
     * saveChat
     * saves all the previous chat into Chat History.txt
     * @throws IOException
     */
    private static void saveChat() throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter("Chat History.txt"));
        String chat = previousChat.toString();
        if (chat.length() > 5000) {
            chat = chat.substring(chat.length() - 5000);
            int index = chat.indexOf('\n');
            chat = chat.substring(index + 1);
        }
        output.write(chat);
        output.close();
    }

    /**
     * Go
     * Starts the server and accepts users
     */
    public void go() {
        // debug System.out.println("Waiting for a client connection..");

        Socket client = null;//hold the client connection

        try {
            serverSock = new ServerSocket(5000);  //assigns a port to the server
            gui.appendToConsole("Port: " + serverSock.getLocalPort());
            serverSock.setSoTimeout(60000);  //60 second timeout
            while (running) {  //loops to accepts client
                client = serverSock.accept();  //wait for connection
                // debug System.out.println("Client connected");
                gui.appendToConsole("Client Connected - " + client.getLocalAddress());
                // create a new user
                User user = new User();
                users.add(user); // add user to the list
                // create a connection handler for the user
                ConnectionHandler connect = new ConnectionHandler(client, user);
                connectionHandlers.add(connect);
                Thread t = new Thread(connect); //create a thread for the new client and pass in the connection handler
                t.start(); //start the new thread
            }
        } catch (Exception e) {
            System.out.println("Error accepting connection");
            //close all and quit
            try {
                client.close();
            } catch (Exception e1) {
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
         * Constructor, initializing input/output streams
         * @param the socket belonging to this client connection
         */
        ConnectionHandler(Socket s, User user) {
            this.user = user;// set user
            this.client = s;  //constructor assigns client to this
            try {  //assign all connections to client
                this.output = new PrintWriter(client.getOutputStream());
                InputStreamReader stream = new InputStreamReader(client.getInputStream());
                this.input = new BufferedReader(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            running = true;
        } //end of constructor


        /* run
         * executed on start of thread
         * it will handle the user inputs and will close when user leaves
         */
        public void run() {

            //Get a message from the client
            String userInput = "";

            //Get a message from the client
            while (running) {  // loop unit a message is received
                try {
                    if (this.input.ready()) { //check for an incoming message
                        userInput = this.input.readLine();  //get a message from the client

                        // debug System.out.println("msg from client: " + userInput);

                        // if the user sends a disconnect
                        if (userInput.startsWith(COMMAND_QUIT)) {
                            writeToUsers(SERVER_MSG + user.getName() + " has disconnected",0,0);
                            running = false; //stop receiving messages
                        } else if (userInput.startsWith(COMMAND_NEW)) { // if the user first joins
                            user.setName(userInput.substring(COMMAND_LEN));// get the user name by substringing everything past the new user command
                            user.setListNum(connectionHandlers.size()); // set the list number for the user
                            gui.appendToConsole("User " + user.getListNum() + " name set: " + userInput.substring(COMMAND_LEN));
                            updateUserList();// send all the users a list of every user on the server for them to update
                            writeToUsers(SERVER_MSG + user.getName() + " has connected",0,0);
                            write(PREVIOUS_CHAT + previousChat.toString()+PREVIOUS_CHAT); // give user the previous chat
                        } else if (userInput.startsWith(COMMAND_MSG)) {// if the user sends a message
                            String msg = userInput.substring(COMMAND_LEN);// remove the command from the beginning
                            int nIndex = msg.indexOf('n');// the first n designates the end of the user's list number
                            // get the receiver's user number
                            int receiverNum = Integer.parseInt(msg.substring(0, nIndex));
                            msg = msg.substring(nIndex + 1);// get the whole message
                            writeToUsers(msg, receiverNum, user.getListNum());
                            if (receiverNum == 0) {// if the user wants to write to the general chat
                                gui.appendToConsole(user.getListNum() + " sent msg to general chat: " + msg);
                                addChat("\n" + user.getName() + ":" + msg);// add message to chat history
                            } else {// if user wants to send a private message
                                gui.appendToConsole(user.getListNum() + " sent  private msg to " + receiverNum + ": " + msg);
                            }
                        } else {
                            output.println("err Command Not Available");
                            output.flush();
                        }

                    }
                } catch (IOException e) {
                    System.out.println("Failed to receive userInput from the client");
                    e.printStackTrace();
                }
            }

            updateUserRemoved();

            updateUserList();

            close();
        } // end of run()

        /**
         * updateUserRemoved
         * remove user from the list of user's and the user's connection handler
         */
        private void updateUserRemoved() {
            gui.appendToConsole(user.getListNum() + "- disconnecting");

            users.remove(user);
            connectionHandlers.remove(this);

            // loop through all the connectionHandlers and update the user's numbers
            for (int i = 1; i <= connectionHandlers.size(); i++) {
                connectionHandlers.get(i).user.setListNum(i);
            }
        }

        /**
         * updateUserList
         * create a list of all the users on the server and send it to every user
         */
        private void updateUserList() {
            StringBuilder string = new StringBuilder();
            string.append(UPDATE_USER_COMMAND); // start with the update user command
            for (int i = 0; i < connectionHandlers.size(); i++) {
                if (i < connectionHandlers.size()-1){// if this is not the last user add a comma
                    string.append(connectionHandlers.get(i).user.getName()+",");
                }else{
                    string.append(connectionHandlers.get(i).user.getName());
                }
            }
            writeToUsers(string.toString(), 0, user.getListNum());
        }

        /**
         * writeToUser
         * write to users, can be to general chat or via private message, message will be sent to the sender as well to confirm that their message has gone through the server
         * @param msg - message user wants to send
         * @param receiver - receiver of the message (0 if its general chat)
         * @param sender - sender of the message
         */
        private void writeToUsers(String msg, int receiver, int sender) {
            if (receiver == 0) {// write to general chat
                if ( ( !msg.startsWith(UPDATE_USER_COMMAND) ) && ( !msg.startsWith(SERVER_MSG) ) && ( !msg.startsWith(KICK_COMMAND)) ) {// if the message does not have a command attached to it already then add msg command
                    msg = COMMAND_MSG + sender + 'n' + msg;
                }
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    connectionHandlers.get(i).write(msg);// write the message to every user
                }
            } else {// write private message
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    String out = PRIVATE_MSG_COMMAND + sender + 'n' + msg;
                    if ((connectionHandlers.get(i).user.getListNum()) == sender || (connectionHandlers.get(i).user.getListNum() == receiver)) {// find reciever and sender
                        connectionHandlers.get(i).write(out);
                    }
                }
            }
        }

        /**
         * write
         * output to the user a message
         * @param msg - message to be sent
         */
        private void write(String msg) {
            output.println(msg);
            output.flush();
        }

        /**
         * close
         * close the current connection handler
         */
        private void close() {
            //close the socket
            try {
                gui.appendToConsole(user.getListNum() + " - " + user.getName() + " has disconnected.");
                this.input.close();
                output.close();
                client.close();
            } catch (Exception e) {
                System.out.println("Failed to close socket");
            }
        }
    } //end of inner class

    /**
     * [consoleThread.java]
     * gets and processes inputs from the console
     */
    private static class consoleThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (serverIn == null) {// if there is no message keep looping
                    continue;
                }
                if (serverIn.equals("close")) {// shuts down server
                    for (int i = 0; i < connectionHandlers.size(); i++) {// send message to every user that server is closing
                        connectionHandlers.get(i).write(SERVER_MSG + "~~SERVER CLOSING IN 20 SECONDS~~");
                        connectionHandlers.get(i).write(COMMAND_QUIT);
                    }
                    gui.appendToConsole("Shutting down server...");
                    // debug System.out.println("Starting shut down");
                    try {// wait 5 seconds
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    // debug System.out.println("Waiting Done");
                    for (int i = 0; i < connectionHandlers.size(); i++) {// turn off all users connection handler's
                        connectionHandlers.get(i).running = false;
                    }
                    // save the chat history
                    try {
                        saveChat();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // close the server socket
                    try {
                        serverSock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    // debug System.out.println("Shutting down");
                    System.exit(1);// close
                } else if (serverIn.startsWith("kick")) { // kick a yser
                    try {
                        serverIn = serverIn.substring(5);// substring kick and number after
                    } catch (StringIndexOutOfBoundsException e) {
                        gui.appendToConsole("No user specified");
                        serverIn = null;
                        continue;
                    }
                    int kickNum;
                    try {// get the list number of the kick user
                        kickNum = Integer.parseInt(serverIn);
                    } catch (StringIndexOutOfBoundsException e) {// if there is no valid number
                        gui.appendToConsole("No user specified");
                        serverIn = null;
                        continue;
                    }
                    ConnectionHandler connectionHandler = null;
                    for (int i = 0; i < connectionHandlers.size(); i++) {// get the connection handler of that user
                        if (connectionHandlers.get(i).user.getListNum() == kickNum) {
                            connectionHandler = connectionHandlers.get(i);
                            break;
                        }
                    }
                    if (connectionHandler == null) {// if there is no connection handler
                        gui.appendToConsole("This is not a valid user");
                        serverIn = null;
                        continue;
                    }
                    // send out that user has been kicked
                    connectionHandler.writeToUsers(SERVER_MSG + connectionHandler.user.getName() + " has been kicked from the chat", 0, kickNum);
                    connectionHandler.write(KICK_COMMAND);
                    connectionHandler.running = false; // stop user's connection handler
                    gui.appendToConsole("Kicked user: " + kickNum + " - " + connectionHandler.user.getName());
                    serverIn = null;
                } else if (serverIn.startsWith("list members")) {// to display the list of all the members on the server
                    StringBuilder listOfMembers = new StringBuilder();
                    // if there are no users online
                    if (users.size() == 0){
                        gui.appendToConsole("There are no users online.");
                        break;
                    }
                    // get the names of all the users in the server
                    for (int i = 0; i < users.size(); i++) {
                        listOfMembers.append(" " + users.get(i).getListNum() + " - " + users.get(i).getName() + " ");
                        if (i < users.size() - 1) {
                            listOfMembers.append(",");
                        }
                    }
                    gui.appendToConsole(listOfMembers.toString());
                    serverIn = null;
                } else if (serverIn.startsWith("broadcast")) {// broadcast a message to general chat
                    String msg = SERVER_MSG + serverIn.substring(9);// add command and get the message, substring first 9 characters of serverIn to remove broadcast
                    for (int i = 0; i < connectionHandlers.size(); i++) {// send to each user
                        connectionHandlers.get(i).write(msg);
                    }
                    serverIn = null;
                } else {// if the input is not valid
                    gui.appendToConsole("This is not a valid command!");
                    serverIn = null;
                }
            }
        }
    }

} //end of Server class