///////////////////////////////////////////////////
// This program is used to communicate with the  //
// Arduino over a serial connection. It sets the //
// time of the DS3231 RTC accurately using       //
// LocalDateTime function from Java.             //
//                                               //
// Code by LsHallo 2016-11-19                    //
// Based on the RXTX sample from Arduino         //
// http://playground.arduino.cc/Interfacing/Java //
//                                               //
// Using the 64-bit RXTX Library from fizzed     //
// http://fizzed.com/oss/rxtx-for-java           //
///////////////////////////////////////////////////

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Enumeration;

import javax.swing.JOptionPane;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;


public class DS3231Sync implements SerialPortEventListener {
	static SerialPort serialPort;
	/**
	* A BufferedReader which will be fed by a InputStreamReader 
	* converting the bytes into characters 
	* making the displayed results codepage independent
	*/
	private BufferedReader input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;
	
	/**
	 * This method initializes the program and opens the serial connection.
	 */
	public void initialize() {
		CommPortIdentifier portId = null;
		Enumeration<?> portEnum = CommPortIdentifier.getPortIdentifiers();

        /** The port we're normally going to use. */
		final String PORT_NAMES[] = {Port};
		
		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			f.printError("Could not find specified COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			f.printError(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized static void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
			f.printInfo("Serial port closed.");
		} else {
			f.printInfo("Serial port already closed.");
		}
	}

	LocalDateTime now;
	boolean last = false, prelast = false; //Not the most beautiful way, but it works :D
	/**
	 * This function is called if a serialEvent is triggered (i.E. data received)
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if(oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				//Read incoming data
				String inputLine = input.readLine();
				
				//Get the local time
				now = LocalDateTime.now();
				
				//Add the time zone offset in seconds, because other method was not working...
				LocalDateTime offTime = now.plusSeconds(zO.getTotalSeconds());
				
				if(inputLine.equals("Skip time set(Y/N)?")) {
					output.write((int)'N');
					return;
				}
				
				//If YEAR requested send it back
				if(inputLine.equals("YEAR")) {
					int year = Integer.parseInt(String.valueOf(offTime.getYear()).substring(2));
					output.write(year);
					return;
				}
				
				//If MONTH requested send it back
				if(inputLine.equals("MONTH")) {
					int month = offTime.getMonthValue();
					output.write(month);
					return;
				}
				
				//If MONTHDAY(Day of the month) requested send it back
				if(inputLine.equals("MONTHDAY")) {
					int day = offTime.getDayOfMonth();
					output.write(day);
					return;
				}
				
				//If WEEKDAY(Day of the week) requested send it back
				if(inputLine.equals("WEEKDAY")) {
					int day = offTime.getDayOfWeek().getValue()+1;
					output.write(day);
					return;
				}
				
				//If HOUR requested send it back
				if(inputLine.equals("HOUR")) {
					int hour = offTime.getHour();
					output.write(hour);
					return;
				}
				
				//If MINUTE requested send it back
				if(inputLine.equals("MINUTE")) {
					int min = offTime.getMinute();
					output.write(min);
					return;
				}
				
				//If SECOND requested send it back
				if(inputLine.equals("SECOND")) {
					int second = offTime.getSecond();
					output.write(second);
					prelast = true;
					return;
				}
				
				//If not the last line print it
				if(!last) {
					System.out.println(inputLine);
					f.printInfo(inputLine);
				}
				
				//If the last item was requested print the line and finished separator
				if(last) {
					f.printInfo(inputLine);
					System.out.println("TIME SET FINISHED\n---------------\n");
					//f.aTextArea.append("FINISHED\n-------------------------\n\n");
					last = false;
					//Print time in the same format as the Arduino does... (Use DateFormatter or something like this?)
					f.printInfo("Time in Java: " + (now.getHour()<10?"0" + now.getHour():now.getHour())
														+ ":" + (now.getMinute()<10?"0" + now.getMinute():now.getMinute())
														+ ":" + (now.getSecond()<10?"0" + now.getSecond():now.getSecond())
														+ " " + (now.getDayOfMonth()<10?"0"+now.getDayOfMonth():now.getDayOfMonth())
														+ "-" + (now.getMonthValue()<10?"0"+now.getMonthValue():now.getMonthValue())
														+ "-" + now.getYear());
				}
				
				if(prelast) {
					last = true;
					prelast = false;
				}
			} catch (Exception e) {
				f.printError("Could not receive byte...");
			}
		}
	}

	static void init() {
		main = new DS3231Sync();
		main.initialize();
		Thread t = new Thread() {
			public void run() {
				//the following line will keep this app alive for 1000 seconds,
				//waiting for events to occur and responding to them (printing incoming messages to console).
				try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
			}
		};
		t.start();
		f.printInfo("Started");
		serialPort.disableReceiveTimeout();
		try {
			serialPort.enableReceiveThreshold(1);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
	}
	
	static Frame f;
	static String Port;
	static String timeZoneOffset;
	static ZoneOffset zO;
	public static DS3231Sync main;
	public static void main(String[] args) throws Exception {
		//Write log to log.txt
		PrintStream out = new PrintStream(new FileOutputStream("log.txt", false), true);
		System.setOut(out);
		
		Port = JOptionPane.showInputDialog("Enter the port number of your Arduino (e.g. COM20; /dev/ttyUSB0): \n (Please watch out: CaSe-SeNsItIvE)");
		if(Port == null || Port.isEmpty()) {
			System.exit(1);
		}
		
		boolean success = true;
		do {
			try {
				success = true;
				timeZoneOffset = JOptionPane.showInputDialog("Enter the time zone offset (Offset of your current computer time. E.g.: +08:30, -05:34, +09:00):\n(Enter \"+00:00\" to skip)");
				if(timeZoneOffset == null || timeZoneOffset.isEmpty()) {
					System.exit(1);
				} else {
					zO = ZoneOffset.of(timeZoneOffset);
				}
			} catch (DateTimeException e) {
				JOptionPane.showMessageDialog(null, "Error, converting to timezone! Please try again.\nWatch out for leading zeros and the + or - symbol! (+06:30 instead of +6:30)", "Error in time zone conversion!", 3);
				success = false;
			}
		} while(!success);
		f = new Frame();
		init();
	}
}