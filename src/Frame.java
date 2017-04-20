import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class Frame extends JFrame {
	private static final long serialVersionUID = -8047177800471601563L;
	JTextArea aTextArea = new JTextArea();
	
	/**
	 * Construct the small output window with the exit button
	 */
	public Frame() {
		//Some related title
		setTitle("RTC Time Sync");
		//Define a default size
		setSize(600, 800);
		//BorderLayout because it's easy to use
		setLayout(new BorderLayout());
		
		//Only output -> editable = false
		aTextArea.setEditable(false);
		//Autoscroll to bottom on new content
		DefaultCaret caret = (DefaultCaret)aTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		//Add to the frame, inside a JScrollPane
		add(new JScrollPane(aTextArea), BorderLayout.CENTER);
		
		//Add the exit button for safe shutdown on linux based systems
		JButton btnExit = new JButton("Exit");
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DS3231Sync.close();
				System.exit(1);
			}
		});
		
		JButton btnReset = new JButton("Reset Connection");
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearOutput();
				printInfo("Resetting connection...");
				DS3231Sync.close();
				DS3231Sync.init();
			}
		});
		btnReset.setToolTipText("Click this button to start the serial connection again. The time sync process will initiate again.");
		
		JButton btnClose = new JButton("Close Connection");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearOutput();
				printInfo("Closing serial port...");
				DS3231Sync.close();
			}
		});
		btnClose.setToolTipText("Close the serial connection to free the serial port for other purposes.");
		
		//Catch the close operation and shut down the application safely
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent wE) {}
			public void windowClosed(WindowEvent wE) {}
			public void windowDeactivated(WindowEvent wE) {}
			public void windowDeiconified(WindowEvent wE) {}
			public void windowIconified(WindowEvent wE) {}
			public void windowOpened(WindowEvent wE) {}

			@Override
			public void windowClosing(WindowEvent wE) {
				DS3231Sync.close();
				System.exit(1);				
			}
	
		});
		
		JPanel jpnlSouth = new JPanel();
		jpnlSouth.setLayout(new BorderLayout());
		//Add the exit and reset button to the frame
		jpnlSouth.add(btnReset, BorderLayout.NORTH);
		jpnlSouth.add(btnClose, BorderLayout.CENTER);
		jpnlSouth.add(btnExit, BorderLayout.SOUTH);
		//Finally make the frame visible
		add(jpnlSouth, BorderLayout.SOUTH);
		setVisible(true);
	}
	
	void printInfo(String information) {
		aTextArea.append("[INFO]: " + information + "\n");
		System.out.println("[INFO]: " + information);
	}
	
	void printWarning(String warning) {
		aTextArea.append("[WARN]: " + warning + "\n");
		System.err.println("[WARN]: " + warning);
	}
	
	void printError(String error) {
		aTextArea.append("[ERROR]: " + error + "\n");
		System.err.println("[ERROR]: " + error);
	}
	
	void clearOutput() {
		aTextArea.setText("");
	}
}