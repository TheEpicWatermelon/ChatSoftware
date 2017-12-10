// imports
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * [Gui.java]
 * runs the gui console for the server
 * @author Sasha Maximovitch
 * @date November 9th, 2017
 */
public class Gui extends JFrame {

    // global variables
    JFrame window;// main window
    JPanel mainPanel;// panel to be put in window
    JPanel topPanel;// panel on top will hold the console text area
    JPanel bottomPanel;// bottom panel that will hold a textfield and button
    JTextArea console;// text area for console
    JScrollPane scrollPane;// scroll for console textarea
    JTextField consoleInput;// text field for console input
    JButton okButton;// button that will send the text from consoleInput to be processed
    String inputText;// text that will hold the text from consoleInput

    Gui(){
        // create window and set it
        super("Console");
        this.window = this;
        this.setSize(400,400);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        // create Panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));


        // create top Panel
        topPanel = new JPanel();

        // create console text area
        console = new JTextArea("~Server Start", 20,33);
        scrollPane = new JScrollPane(console);
        console.setEditable(false);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // add console to the top panel
        topPanel.add(scrollPane);

        // bottom panel
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());

        // create input for the console
        consoleInput = new JTextField(20);
        bottomPanel.add(consoleInput);

        // create button for console input
        okButton = new JButton("Ok");
        okButton.addActionListener(new OkButtonListener());
        bottomPanel.add(okButton);

        // add both top and bottom panel to the main panel
        mainPanel.add(topPanel);
        mainPanel.add(bottomPanel);

        // add the main panel to the frame
        this.add(mainPanel);
        // turn on gui
        this.setVisible(true);

    }

    /**
     * [OkButtonListener.java]
     * Action Listener for ok button that will send the input text
     */
    class OkButtonListener implements ActionListener {
        @Override
        /**
         * actionPerformed
         * Runs when the button is clicked
         */
        public void actionPerformed(ActionEvent e) {
            inputText = consoleInput.getText(); // get the input text
            consoleInput.setText(""); // reset the console input
            Server.serverIn = inputText;// set the serverIn in Server.java to the new console input
            appendToConsole(inputText);// add the input to the console text area
            inputText = null; // reset inputText
            // debug System.out.println("Clicked");
        }
    }

    /**
     * appendToConsole
     * takes in text and adds it to the text area of the console
     * @param text
     */
    public void appendToConsole(String text){
        console.append("\n" +text);// add the text to the JTextArea
        this.repaint();// repaint and re-validate
        this.revalidate();
        JScrollBar vertical = scrollPane.getVerticalScrollBar();// makes it so that the scroll bar automatically scrolls to the bottom
        vertical.setValue( vertical.getMaximum() );
    }
}
