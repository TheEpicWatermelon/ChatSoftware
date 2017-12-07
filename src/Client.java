import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.JButton;
import java.awt.Color;
import javax.swing.border.LineBorder;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * Client.java
 * Window for client interactions
 * Aaron Ng
 * Nov 30, 2017
 */
public class Client extends JFrame {
	// Swing Components
	private JPanel pnlSidebar;
	private JLabel lblTitle;
	private JScrollPane scrollPaneChat;
	private JTextArea txtAreaMessage;
	private JButton btnSend;
	private JButton btnDisconnect;
	private JLabel lblChatList;
	private JScrollPane scrollPaneMembers;
	private JList <String> listMembers;
	private ArrayList <JTextArea> txtArea;
	// Server Components
	private Socket mySocket;
	private BufferedReader input;
	private PrintWriter output;
	private String name;
	// Constants and Runtime Variables
	private boolean running;
	private int userNum;
	private int channelIndex;
	private static final String COMMAND_START = "svn";
	private static final String NEW_USER_COMMAND = "cnu";
	private static final String COMMAND_QUIT = "svt";
	private static final String COMMAND_USER_LEAVE = "cul";
	private static final String COMMAND_MESSAGE = "msg";
	private static final String COMMAND_PM = "pmg";
	private static final String SERVER_MSG = "smg";
	private static final String KICK_COMMAND = "kck";
	private JLabel lblMembers2;

	/*
	 * Client
	 * Constructor for client window
	 * @param none
	 * @returns none
	 */
	public Client(Socket sk, BufferedReader in, PrintWriter out, String n) {
		mySocket = sk;
		input = in;
		output = out;
		name = n;
		initComponents(); // Method to create UI
		createEvents(); // Method to create actionListeners
		output.println(COMMAND_START + name); // Give server the client name
		output.flush();
		running = true;
		Thread listen = new Thread(new MessageReciever()); // Start listening for messages from the server
		listen.start();
//		(new Thread(new ChatHistory())).start(); // Get chat history for general chat from the server
//		(new Thread(new ChatLimiter())).start(); // Limit all chat boxes to 5000 characters
	} // End Client constructor

	/**
	 * createEvents
	 * Method to create actionListeners
	 * @param none
	 * @returns none
	 */
	private void createEvents() {
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!(txtAreaMessage.getText()).equals("")) { // Only send message if there is text inside the text area
					output.println(COMMAND_MESSAGE + channelIndex + "n" + txtAreaMessage.getText());
					output.flush();
					txtAreaMessage.setText("");
				} // End if
			} // End actionPerformed method
		});
		listMembers.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if (!listMembers.getValueIsAdjusting()) {
					if (listMembers.getSelectedIndex() != userNum) {
						channelIndex = listMembers.getSelectedIndex();
						scrollPaneChat.setViewportView(txtArea.get(channelIndex));
						lblTitle.setText(listMembers.getSelectedValue());
					} // End if
				} // End if
			}
		});
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				output.println(COMMAND_QUIT);
				output.flush();
				running = false;
				try {
					input.close();
					output.close();
					mySocket.close();
				}catch(Exception e) {
					System.out.println("Failed to close");
				}
				System.exit(0);
			}
		});
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				output.println(COMMAND_QUIT);
				output.flush();
				running = false;
				try {
					input.close();
					output.close();
					mySocket.close();
				}catch(Exception e) {
					System.out.println("Failed to close");
				}
				System.exit(0);
			}
		});
	} // End createEvents method

	/**
	 * initComponents
	 * Initialize swing components for UI
	 * @param none
	 * @returns none
	 */
	private void initComponents() {

		this.setSize(800, 600);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		lblTitle = new JLabel("General Chat");
		lblTitle.setFont(new Font("Arial", Font.PLAIN, 20));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblChatList = new JLabel("Chat List");
		lblChatList.setFont(new Font("Arial", Font.PLAIN, 16));
		lblChatList.setHorizontalAlignment(SwingConstants.CENTER);

		txtAreaMessage = new JTextArea("");
		btnSend = new JButton("Send");
		btnSend.setFont(new Font("Arial Black", Font.PLAIN, 16));  
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setFont(new Font("Arial", Font.PLAIN, 12));
		
		scrollPaneMembers = new JScrollPane();
		scrollPaneMembers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listMembers = new JList(new String[] {"Gen"});
		scrollPaneMembers.setViewportView(listMembers);
		listMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPaneChat = new JScrollPane();
		scrollPaneChat.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPaneChat.setViewportBorder(new LineBorder(new Color(0, 0, 0)));

		JLabel lblServerIp = new JLabel("Server IP:");
		JLabel lblServerIP2 = new JLabel(mySocket.getLocalAddress().toString().substring(1));
		JLabel lblPort = new JLabel("Port:");
		JLabel lblPort2 = new JLabel(Integer.toString(mySocket.getPort()));
		JLabel lblMembers = new JLabel("Members: ");
		lblMembers2 = new JLabel(Integer.toString(listMembers.getModel().getSize()-1));
		
		pnlSidebar = new JPanel();
		GroupLayout gl_pnlSidebar = new GroupLayout(pnlSidebar);
		gl_pnlSidebar.setHorizontalGroup(
				gl_pnlSidebar.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlSidebar.createSequentialGroup()
						.addGroup(gl_pnlSidebar.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING, gl_pnlSidebar.createParallelGroup(Alignment.TRAILING)
										.addGroup(Alignment.LEADING, gl_pnlSidebar.createSequentialGroup()
												.addContainerGap()
												.addComponent(scrollPaneMembers, GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE))
										.addGroup(gl_pnlSidebar.createSequentialGroup()
												.addGap(18)
												.addComponent(lblChatList, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
								.addGroup(gl_pnlSidebar.createSequentialGroup()
										.addContainerGap()
										.addComponent(lblServerIp)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(lblServerIP2))
								.addGroup(gl_pnlSidebar.createSequentialGroup()
										.addContainerGap()
										.addGroup(gl_pnlSidebar.createParallelGroup(Alignment.LEADING)
												.addComponent(lblMembers)
												.addComponent(lblPort))
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addGroup(gl_pnlSidebar.createParallelGroup(Alignment.LEADING)
												.addComponent(lblPort2)
												.addComponent(lblMembers2))))
						.addContainerGap())
				);
		gl_pnlSidebar.setVerticalGroup(
				gl_pnlSidebar.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_pnlSidebar.createSequentialGroup()
						.addGap(18)
						.addComponent(lblChatList)
						.addGap(18)
						.addComponent(scrollPaneMembers, GroupLayout.PREFERRED_SIZE, 262, GroupLayout.PREFERRED_SIZE)
						.addGap(18)
						.addGroup(gl_pnlSidebar.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblServerIp)
								.addComponent(lblServerIP2))
						.addGap(18)
						.addGroup(gl_pnlSidebar.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblPort)
								.addComponent(lblPort2))
						.addGap(18)
						.addGroup(gl_pnlSidebar.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblMembers)
								.addComponent(lblMembers2))
						.addContainerGap(85, Short.MAX_VALUE))
				);
		pnlSidebar.setLayout(gl_pnlSidebar);

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(pnlSidebar, GroupLayout.PREFERRED_SIZE, 166, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
										.addComponent(lblTitle, GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE)
										.addGap(14)
										.addComponent(btnDisconnect)
										.addContainerGap())
								.addGroup(groupLayout.createSequentialGroup()
										.addGap(18)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(scrollPaneChat, GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
														.addContainerGap())
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(txtAreaMessage, GroupLayout.PREFERRED_SIZE, 441, GroupLayout.PREFERRED_SIZE)
														.addGap(18)
														.addComponent(btnSend, GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
														.addGap(24))))))
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblTitle, GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
										.addGap(8)
										.addComponent(btnDisconnect)))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(pnlSidebar, 0, 0, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
										.addComponent(scrollPaneChat, GroupLayout.PREFERRED_SIZE, 413, GroupLayout.PREFERRED_SIZE)
										.addGap(18)
										.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
												.addComponent(txtAreaMessage, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
												.addComponent(btnSend, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE))))
						.addContainerGap())
				);
		
		channelIndex = 0;
		txtArea = new ArrayList <JTextArea>();
		txtArea.add(new JTextArea(""));
		txtArea.get(channelIndex).setEditable(false);
		txtArea.get(channelIndex).setFont(new Font("Century Gothic", Font.PLAIN, 16));
		txtArea.get(channelIndex).setWrapStyleWord(true);
		txtArea.get(channelIndex).setLineWrap(true);
		scrollPaneChat.setViewportView(txtArea.get(channelIndex));
		getContentPane().setBackground(new Color(142, 185, 255));
		getContentPane().setLayout(groupLayout);
	} // End initComponents method

	/**
	 * MessageReciever.java
	 * Inner class used to listen to server sent messages
	 * Aaron Ng
	 * Dec 1, 2017
	 */
	class MessageReciever implements Runnable{
		boolean firstCNU = true;
		MessageReciever(){} // End MessageReciever constructor
		/**
		 * run
		 * Method for listening to received messages
		 * @param none
		 * @returns none
		 */
		public void run() {
			while(running) {
				try {
					if (input.ready()) {
						String msg;  
						msg = input.readLine();
						if (msg.startsWith(COMMAND_MESSAGE)) { // Append message to respective text area
							
							System.out.println("msg from server: " + msg);
							msg = msg.substring(3);
							msg = listMembers.getModel().getElementAt(Integer.parseInt(msg.substring(0, msg.indexOf("n")))) + ": " + msg.substring(msg.indexOf("n") + 1);
							txtArea.get(0).append("\n" + msg);
							System.out.println("msg to general: " + msg);
							
						}else if (msg.equals(COMMAND_QUIT)) { // Server shut down
							
							running = false;
							try {
								input.close();
								output.close();
								mySocket.close();
							}catch(Exception e) {
								System.out.println("Failed to close");
							}
							JOptionPane.showMessageDialog(null, "Server has been shut down", "Server Down", JOptionPane.ERROR_MESSAGE);
							System.exit(0);
							
						}else if (msg.startsWith(NEW_USER_COMMAND)){ // New user has connected; Add their name to list
							
							msg = msg.substring(3);
							if(firstCNU) { // If user has just connected, send full member list
								ArrayList <String> list = new ArrayList <String> ();
								list.add("General");
								while(msg.indexOf(",") != -1) { // Loop through to get all clients
									txtArea.add(new JTextArea(""));
									list.add(msg.substring(0, msg.indexOf(",")));
									msg = msg.substring(msg.indexOf(",") + 1);
								} // End while loop
								list.add(msg);
								txtArea.add(new JTextArea(""));
								String [] tmp = list.toArray(new String [list.size()]);
								System.out.println("Old List; Size:" + listMembers.getModel().getSize());
								listMembers =  new JList(tmp);
								System.out.println("new user; Size:" + listMembers.getModel().getSize());
								scrollPaneMembers.setViewportView(listMembers);
								System.out.println("updated list set to viewport");

								userNum = tmp.length -1;
								listMembers.addListSelectionListener(new ListSelectionListener() {
									public void valueChanged(ListSelectionEvent arg0) {
										if (!listMembers.getValueIsAdjusting()) {
											if (listMembers.getSelectedIndex() != userNum) {
												channelIndex = listMembers.getSelectedIndex();
												scrollPaneChat.setViewportView(txtArea.get(channelIndex));
												lblTitle.setText((String) listMembers.getSelectedValue());
											} // End if
										} // End if
									}
								});
								
							}else { // Else add 1 new member to bottom of list
								String [] tmp = new String [listMembers.getModel().getSize() + 1];
								for (int i = 0; i < listMembers.getModel().getSize(); i++) {
									tmp[i] = listMembers.getModel().getElementAt(i);
								} // End for loop
								tmp[tmp.length - 1] = msg;
								txtArea.add(new JTextArea(""));	
								listMembers = new JList(tmp);
								scrollPaneMembers.setViewportView(listMembers);
								lblMembers2.setText("" + (listMembers.getModel().getSize() - 1));
								listMembers.addListSelectionListener(new ListSelectionListener() {
									public void valueChanged(ListSelectionEvent arg0) {
										if (!listMembers.getValueIsAdjusting()) {
											if (listMembers.getSelectedIndex() != userNum) {
												channelIndex = listMembers.getSelectedIndex();
												scrollPaneChat.setViewportView(txtArea.get(channelIndex));
												lblTitle.setText((String) listMembers.getSelectedValue());
											} // End if
										} // End if
									}
								});

							} // End if
							
						}else if (msg.startsWith(COMMAND_USER_LEAVE)){ // User at index has disconnected, remove them from list
							
							int index = Integer.parseInt(msg.substring(3)); // Get index of left user
							txtArea.remove(index);// Remove user's chat box from txtArea ArrayList
							String [] tmp = new String[listMembers.getModel().getSize() - 1];
							for (int i = 0; i < listMembers.getModel().getSize(); i++) {
								if (i != index) {
									tmp[i] = listMembers.getModel().getElementAt(i);
								} // End if
							} // End for loop
							
						}else if (msg.startsWith(COMMAND_PM)){
							
							System.out.println("msg from server: " + msg);
							msg = msg.substring(3);
							int index = Integer.parseInt(msg.substring(msg.indexOf("n")));
							txtArea.get(index).append(listMembers.getModel().getElementAt(index) + msg.substring(msg.indexOf("n") + 1));
							
						}else if (msg.startsWith(SERVER_MSG)){
							
							txtArea.get(0).append("\n" + msg.substring(3));
							
						}else if (msg.startsWith(KICK_COMMAND)){
							
							running = false;
							try {
								input.close();
								output.close();
								mySocket.close();
							}catch(Exception e) {
								System.out.println("Failed to close");
							}
							JOptionPane.showMessageDialog(null, "You Have Been Kicked", "Kicked From Server", JOptionPane.ERROR_MESSAGE);
							System.exit(0);
							
						} // End if
					} // End if
				}catch (IOException e) { 
					System.out.println("Failed to receive msg from the server");
					e.printStackTrace();
				} // End try catch statement
			} // End while loop
		} // End run method
	} // End MessageReciever class

	/**
	 * ChatHistory.java
	 * Inner class used to receive chat history for general chat
	 * Aaron Ng
	 * Dec 1, 2017
	 */
	class ChatHistory implements Runnable{
		boolean going = true;
		ChatHistory(){} // End ChatHistory Constructor
		/**
		 * run
		 * Method for listening to received messages
		 * @param none
		 * @returns none
		 */
		public void run() {
			while(going) {
				try {
					if (input.ready()) {
						String msg;          
						msg = input.readLine();
						txtArea.get(0).setText(msg);
						System.out.println("msg from server: " + msg);
						going = false;
					} // End if
				}catch (IOException e) { 
					System.out.println("Failed to receive msg from the server");
					e.printStackTrace();
				} // End try catch statement
			} // End while loop
		} // End run method
	} // End ChatHistory class
} // End Client class
