import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

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
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/*
 * Client.java
 * Window for client interactions
 * Aaron Ng
 * Nov 30, 2017
 */
public class Client extends JFrame {
	private JPanel panel;
	private JLabel lblTitle;
	private JTextArea txtAreaChat;
	private JScrollPane scrollPaneChat;
	private JTextArea txtAreaMessage;
	private JButton btnSend;
	private JLabel lblChatList;
	private JScrollPane scrollPaneMembers;
	private JList <String> listMembers;
	private Socket mySocket;
	private BufferedReader input;
	private PrintWriter output;
	private String name;
	private JButton btnDisconnect;
	public boolean running;

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
		output.println("svn" + name);
		output.flush();
		running = true;
		Thread listen = new Thread(new MessageReciever());
		listen.start();

	} // End Client constructor

	/*
	 * createEvents
	 * Method to create actionListeners
	 * @param none
	 * @returns none
	 */
	private void createEvents() {
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				output.println("msg" + txtAreaMessage.getText());
				output.flush();
			}
		});
		listMembers.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if (!listMembers.getValueIsAdjusting()) {
					output.println("svc" + listMembers.getSelectedIndex());
					output.flush();
					lblTitle.setText(listMembers.getSelectedValue());
					if (listMembers.getSelectedIndex() == 0) {
						(new Thread(new ChatHistory())).start();
					} // End if
				} // End if
			}
		});
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				output.println("svt");
				output.flush();
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
				output.println("svt");
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

	/*
	 * initComponents
	 * Initialize swing components for UI
	 * @param none
	 * @returns none
	 */
	private void initComponents() {

		this.setSize(800, 600);
		lblTitle = new JLabel("General Chat");
		lblTitle.setFont(new Font("Arial", Font.PLAIN, 20));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		panel = new JPanel();

		lblChatList = new JLabel("Chat List");
		lblChatList.setFont(new Font("Arial", Font.PLAIN, 16));
		lblChatList.setHorizontalAlignment(SwingConstants.CENTER);

		scrollPaneMembers = new JScrollPane();
		scrollPaneMembers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		txtAreaMessage = new JTextArea();

		btnSend = new JButton("Send");
		btnSend.setFont(new Font("Arial Black", Font.PLAIN, 16));		
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setFont(new Font("Arial", Font.PLAIN, 12));

		listMembers = new JList(new String[] {"General", "1", "2", "3", "4", "5", "6"});
		scrollPaneMembers.setViewportView(listMembers);
		listMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		txtAreaChat = new JTextArea();
		txtAreaChat.setLineWrap(true);
		txtAreaChat.setWrapStyleWord(true);
		txtAreaChat.setEditable(false);
		scrollPaneChat = new JScrollPane();
		scrollPaneChat.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPaneChat.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
		scrollPaneChat.add(txtAreaChat);

		JLabel lblServerIp = new JLabel("Server IP:");
		JLabel lblServerIP2 = new JLabel(mySocket.getLocalAddress().toString().substring(1));
		JLabel lblPort = new JLabel("Port:");
		JLabel lblPort2 = new JLabel(Integer.toString(mySocket.getPort()));
		JLabel lblMembers = new JLabel("Members: ");
		JLabel lblMembers2 = new JLabel(Integer.toString(listMembers.getModel().getSize()-1));

		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
				gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
						.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING, gl_panel.createParallelGroup(Alignment.TRAILING)
										.addGroup(Alignment.LEADING, gl_panel.createSequentialGroup()
												.addContainerGap()
												.addComponent(scrollPaneMembers, GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE))
										.addGroup(gl_panel.createSequentialGroup()
												.addGap(18)
												.addComponent(lblChatList, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
								.addGroup(gl_panel.createSequentialGroup()
										.addContainerGap()
										.addComponent(lblServerIp)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(lblServerIP2))
								.addGroup(gl_panel.createSequentialGroup()
										.addContainerGap()
										.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
												.addComponent(lblMembers)
												.addComponent(lblPort))
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
												.addComponent(lblPort2)
												.addComponent(lblMembers2))))
						.addContainerGap())
				);
		gl_panel.setVerticalGroup(
				gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
						.addGap(18)
						.addComponent(lblChatList)
						.addGap(18)
						.addComponent(scrollPaneMembers, GroupLayout.PREFERRED_SIZE, 262, GroupLayout.PREFERRED_SIZE)
						.addGap(18)
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblServerIp)
								.addComponent(lblServerIP2))
						.addGap(18)
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblPort)
								.addComponent(lblPort2))
						.addGap(18)
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblMembers)
								.addComponent(lblMembers2))
						.addContainerGap(85, Short.MAX_VALUE))
				);
		panel.setLayout(gl_panel);

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(panel, GroupLayout.PREFERRED_SIZE, 166, GroupLayout.PREFERRED_SIZE)
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
								.addComponent(panel, 0, 0, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
										.addComponent(scrollPaneChat, GroupLayout.PREFERRED_SIZE, 413, GroupLayout.PREFERRED_SIZE)
										.addGap(18)
										.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
												.addComponent(txtAreaMessage, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
												.addComponent(btnSend, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE))))
						.addContainerGap())
				);
		getContentPane().setBackground(new Color(142, 185, 255));
		getContentPane().setLayout(groupLayout);
	} // End initComponents method

	/*
	 * MessageReciever.java
	 * Inner class used to listen to server sent messages
	 * Aaron Ng
	 * Dec 1, 2017
	 */
	class MessageReciever implements Runnable{

		MessageReciever(){} // End MessageReciever constructor
		/*
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
						txtAreaChat.append(msg);
						System.out.println("msg from server: " + msg);   
					} // End if
				}catch (IOException e) { 
					System.out.println("Failed to receive msg from the server");
					e.printStackTrace();
				} // End try catch statement
			} // End while loop
		} // End run method
	} // End MessageReciever class

	/*
	 * ChatHistory.java
	 * Inner class used to receive chat history for general chat
	 * Aaron Ng
	 * Dec 1, 2017
	 */
	class ChatHistory implements Runnable{
		boolean going = true;
		ChatHistory(){} // End ChatHistory Constructor
		/*
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
						txtAreaChat.setText(msg);
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
