import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Gui extends JFrame {

    JFrame window;
    JPanel mainPanel;
    JPanel topPanel;
    JPanel bottomPanel;
    JTextArea console;
    JScrollPane scrollPane;
    JTextField consoleInput;
    JButton okButton;
    String inputText;

    Gui(){
        super("Console");
        this.window = this;

        this.setSize(400,400);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        // create Panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(0,1, 0, 50));

        // create top Panel
        topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        console = new JTextArea("~Server Start", 20,20);
        scrollPane = new JScrollPane(console);

        console.setEditable(false);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);

        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        topPanel.add(scrollPane);

        // bottom panel
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());

        consoleInput = new JTextField(20);
        bottomPanel.add(consoleInput);

        okButton = new JButton("Ok");
        okButton.addActionListener(new OkButtonListener());
        bottomPanel.add(okButton);

        mainPanel.add(topPanel);
        mainPanel.add(bottomPanel);

        this.add(mainPanel);

        this.setVisible(true);

    }

    class OkButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            inputText = consoleInput.getText();
        }
    }

    public void appendToConsole(String text){
        console.append(text);
        this.repaint();
    }
}
