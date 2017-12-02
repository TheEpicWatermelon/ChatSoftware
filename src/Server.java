/* [ChatProgramServer.java]
 * Description: This is an example of a chat server.
 * The program  waits for a client and accepts a message.
 * It then responds to the message and quits.
 * This server demonstrates how to employ multithreading to accepts multiple clients
 * @author Mangat
 * @version 1.0a
 */

//imports for network communication
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Server {

    ServerSocket serverSock;// server socket for connection
    static boolean running = true;  // controls if the server is accepting clients
    static List<User> users = Collections.synchronizedList(new ArrayList<>()); // syncronized list of users
    private static final int COMMAND_LEN = 4;
    private static final String COMMAND_QUIT = "QUT";// add more commands later

    /** Main
     * @param args parameters from command line
     */
    public static void main(String[] args) {
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
            serverSock.setSoTimeout(5000);  //5 second timeout
            while(running) {  //this loops to accept multiple clients
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
                //Note: you might want to keep references to all clients if you plan to broadcast messages
                //Also: Queues are good tools to buffer incoming/outgoing messages
                User user = new User();
                users.add(user);
                Thread t = new Thread(new ConnectionHandler(client, user)); //create a thread for the new client and pass in the socket
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
            String msg="";

            //Get a message from the client
            while(running) {  // loop unit a message is received
                try {
                    if (input.ready()) { //check for an incoming messge
                        msg = input.readLine();  //get a message from the client
                        // Commands: HELLO, MSG, QUIT
                        // Format: COMMAND Something
                        // e.g. HELLO Josh
                        // MSG Hello World
                        // QUIT

                        System.out.println("msg from client: " + msg);

                        if (msg.startsWith(COMMAND_QUIT)) {
                            running = false; //stop receving messages
                        } else if(msg.startsWith("HELLO")){
                            user.setName(msg.substring(COMMAND_LEN));
                        }
                    }
                }catch (IOException e) {
                    System.out.println("Failed to receive msg from the client");
                    e.printStackTrace();
                }
            }

            //Send a message to the client
            output.println("We got your message! Goodbye.");
            output.flush();

            //close the socket
            try {
                input.close();
                output.close();
                client.close();
            }catch (Exception e) {
                System.out.println("Failed to close socket");
            }
        } // end of run()
    } //end of inner class
} //end of Server class