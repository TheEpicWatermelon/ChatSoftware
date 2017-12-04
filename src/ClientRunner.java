import javax.swing.JFrame;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.awt.event.ActionEvent;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Color;

/*
 * ClientRunner.java
 * Launcher for client
 * Aaron Ng
 * Nov 30, 2017
 */
public class ClientRunner {
 private JTextField tfAddress;
 private JTextField tfPort;
 private JTextField tfUsername;
 private JFrame svSetup;
 private JLabel lblErrorMsg;
 private JButton btnConnect;

 /*
  * main
  * Main method
  * @param String[] args
  * @returns none
  */
 public static void main(String[] args) {
  new ClientRunner().setup();
 } // End main method

 /*
  * setup
  * Method to setup client connection to server
  * @param none
  * @returns none
  */
 public void setup() {

  initComponents();

  btnConnect.addActionListener(new ActionListener() {
   public void actionPerformed(ActionEvent ev) {
    try {
     Socket mySocket = new Socket(tfAddress.getText(), Integer.parseInt(tfPort.getText()));
     InputStreamReader stream1 = new InputStreamReader(mySocket.getInputStream());
     BufferedReader input = new BufferedReader(stream1);
     PrintWriter output = new PrintWriter(mySocket.getOutputStream());
     new Client(mySocket, input, output, tfUsername.getText());
     svSetup.dispose();
    } catch (IOException e) {
     lblErrorMsg.setText("Server Connection Error!");
     e.printStackTrace();
    } // End try catch statement
   }
  });

 } // End setup method

 /*
  * initComponents Draw out UI for the window
  * 
  * @param none
  * 
  * @returns none
  */
 private void initComponents() {
  svSetup = new JFrame();
  (svSetup.getContentPane()).setBackground(new Color(142, 185, 255));

  JLabel lblChatSetup = new JLabel("Chat Setup");
  lblChatSetup.setHorizontalAlignment(SwingConstants.CENTER);

  JLabel lblIpAddress = new JLabel("IP Address");

  tfAddress = new JTextField();
  tfAddress.setColumns(10);

  tfPort = new JTextField();
  tfPort.setColumns(10);

  JLabel lblPort = new JLabel("Port:");

  JLabel lblUsername = new JLabel("Username:");

  tfUsername = new JTextField();
  tfUsername.setColumns(10);

  btnConnect = new JButton("Connect");

  lblErrorMsg = new JLabel("");
  lblErrorMsg.setForeground(Color.RED);
  GroupLayout groupLayout = new GroupLayout(svSetup.getContentPane());
  groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
    .addGroup(groupLayout.createSequentialGroup().addGap(65)
      .addComponent(lblChatSetup, GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE).addGap(65))
    .addGroup(groupLayout.createSequentialGroup().addContainerGap()
      .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(lblIpAddress)
        .addComponent(lblPort))
      .addGap(18)
      .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        .addComponent(tfPort, GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
        .addComponent(tfAddress, GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE))
      .addContainerGap())
    .addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblUsername).addGap(18)
      .addComponent(tfUsername, GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE).addContainerGap())
    .addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(lblErrorMsg)
      .addPreferredGap(ComponentPlacement.RELATED, 145, Short.MAX_VALUE).addComponent(btnConnect)
      .addContainerGap()));
  groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
    .createSequentialGroup().addContainerGap().addComponent(lblChatSetup).addGap(18)
    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblIpAddress).addComponent(
      tfAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    .addGap(18)
    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
      .addComponent(tfPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
        GroupLayout.PREFERRED_SIZE)
      .addComponent(lblPort))
    .addGap(18)
    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(lblUsername).addComponent(
      tfUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    .addGap(18).addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(btnConnect)
      .addComponent(lblErrorMsg))
    .addContainerGap(21, Short.MAX_VALUE)));
  svSetup.getContentPane().setLayout(groupLayout);
  svSetup.setLocationRelativeTo(null);
  svSetup.setSize(300, 240);
  svSetup.setVisible(true);
 } // End initComponents Method
} // End Client class
