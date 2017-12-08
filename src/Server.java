/**
 * [Server.java]
 */

//imports

import java.io.*;
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
     * stars gui and server
     *
     * @param args parameters from command line
     */
    public static void main(String[] args) throws IOException {
        // initialize GUI
        gui = new Gui();
        // Start console input listener
        Thread console = new Thread(new consoleThread());
        console.start();
        // get chat history
        previousChat = new StringBuilder();
        previousChat.append(getPrevChat());
        // start the server
        new Server().go(); //start the server
    }

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

    private static void addChat(String msg) {
        previousChat.append(msg);
    }

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
        System.out.println("Waiting for a client connection..");

        Socket client = null;//hold the client connection

        try {
            serverSock = new ServerSocket(5000);  //assigns an port to the server
            gui.appendToConsole("Port: " + serverSock.getLocalPort());
            //serverSock.setSoTimeout(25000);  //5 second timeout
            while (running) {  //loops to accepts client
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
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
            // System.out.println("Error accepting connection");
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
         * Constructor
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

                        System.out.println("msg from client: " + userInput);

                        if (userInput.startsWith(COMMAND_QUIT)) {
                            writeToUsers(SERVER_MSG + user.getName() + " has disconnected",0,0);
                            running = false; //stop receiving messages
                        } else if (userInput.startsWith(COMMAND_NEW)) {
                            user.setName(userInput.substring(COMMAND_LEN));
                            user.setListNum(connectionHandlers.size());
                            gui.appendToConsole("User " + user.getListNum() + " name set: " + userInput.substring(COMMAND_LEN));
                            updateUsers();
                            writeToUsers(SERVER_MSG + user.getName() + " has connected",0,0);
                            write(PREVIOUS_CHAT + previousChat.toString()+PREVIOUS_CHAT);
                        } else if (userInput.startsWith(COMMAND_MSG)) {
                            String msg = userInput.substring(COMMAND_LEN);
                            int nIndex = msg.indexOf('n');
                            // get the receiver's user number
                            int receiverNum = Integer.parseInt(msg.substring(0, nIndex));
                            msg = msg.substring(nIndex + 1);
                            writeToUsers(msg, receiverNum, user.getListNum());
                            if (receiverNum == 0) {
                                gui.appendToConsole(user.getListNum() + " sent msg to general chat: " + msg);
                                addChat("\n" + user.getName() + ":" + msg);
                            } else {
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

            updateUsers();

            close();
        } // end of run()

        private void updateUserRemoved() {
            gui.appendToConsole(user.getListNum() + "- disconnecting");
            users.remove(user);

            connectionHandlers.remove(this);

            for (int i = 0; i < connectionHandlers.size(); i++) {
                connectionHandlers.get(i).user.setListNum(i);
            }
        }

        private void updateUsers() {
            StringBuilder string = new StringBuilder();
            string.append(UPDATE_USER_COMMAND);
            for (int i = 0; i < connectionHandlers.size(); i++) {
                if (i < connectionHandlers.size()-1){
                    string.append(connectionHandlers.get(i).user.getName()+",");
                }else{
                    string.append(connectionHandlers.get(i).user.getName());
                }
            }
            writeToUsers(string.toString(), 0, user.getListNum());
        }

        private void writeToUsers(String msg, int receiver, int sender) {
            if (receiver == 0) {
                if ( ( !msg.startsWith(UPDATE_USER_COMMAND) ) && ( !msg.startsWith(SERVER_MSG) ) && ( !msg.startsWith(KICK_COMMAND)) ) {
                    msg = COMMAND_MSG + sender + 'n' + msg;
                }
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    connectionHandlers.get(i).write(msg);
                }
            } else {
                for (int i = 0; i < connectionHandlers.size(); i++) {
                    String out = PRIVATE_MSG_COMMAND + sender + 'n' + msg;
                    if ((connectionHandlers.get(i).user.getListNum()) == sender || (connectionHandlers.get(i).user.getListNum() == receiver)) {
                        connectionHandlers.get(i).write(msg);
                    }
                }
            }
        }

        private void write(String msg) {
            output.println(msg);
            output.flush();
        }

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


    private static class consoleThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (serverIn == null) {
                    continue;
                }
                if (serverIn.equals("close")) {
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        connectionHandlers.get(i).write(SERVER_MSG + "~~SERVER CLOSING IN 20 SECONDS~~");
                        connectionHandlers.get(i).write(COMMAND_QUIT);
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
                        connectionHandlers.get(i).running = false;
                    }

                    try {
                        saveChat();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        serverSock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    System.out.println("Shutting down");
                    System.exit(1);
                } else if (serverIn.startsWith("kick")) {
                    try {
                        serverIn = serverIn.substring(5);// substring kick and number after
                    } catch (StringIndexOutOfBoundsException e) {
                        gui.appendToConsole("No user specified");
                        serverIn = null;
                        continue;
                    }
                    int kickNum;
                    try {
                        kickNum = Integer.parseInt(serverIn);
                    } catch (StringIndexOutOfBoundsException e) {
                        gui.appendToConsole("No user specified");
                        serverIn = null;
                        continue;
                    }
                    ConnectionHandler connectionHandler = null;
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        if (connectionHandlers.get(i).user.getListNum() == kickNum) {
                            connectionHandler = connectionHandlers.get(i);
                            break;
                        }
                    }
                    if (connectionHandler == null) {
                        gui.appendToConsole("This is not a valid user");
                        serverIn = null;
                        continue;
                    }
                    connectionHandler.writeToUsers(SERVER_MSG + connectionHandler.user.getName() + " has been kicked from the chat", 0, kickNum);
                    connectionHandler.write(KICK_COMMAND);
                    connectionHandler.running = false;
                    gui.appendToConsole("Kicked user: " + kickNum + " - " + connectionHandler.user.getName());
                    // update user numbers
                    serverIn = null;
                } else if (serverIn.startsWith("list members")) {
                    StringBuilder listOfMembers = new StringBuilder();

                    if (users.size() == 0){
                        gui.appendToConsole("There are no users online.");
                        break;
                    }

                    for (int i = 0; i < users.size(); i++) {
                        listOfMembers.append(" " + users.get(i).getListNum() + " - " + users.get(i).getName() + " ");
                        if (i < users.size() - 1) {
                            listOfMembers.append(",");
                        }
                    }
                    gui.appendToConsole(listOfMembers.toString());
                    serverIn = null;
                } else if (serverIn.startsWith("broadcast")) {
                    String msg = SERVER_MSG + serverIn.substring(9);
                    for (int i = 0; i < connectionHandlers.size(); i++) {
                        connectionHandlers.get(i).write(msg);
                    }
                    serverIn = null;
                } else {
                    gui.appendToConsole("This is not a valid command!");
                    serverIn = null;
                }
            }
        }
    }

} //end of Server class