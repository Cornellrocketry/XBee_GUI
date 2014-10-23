import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
//import javax.comm.CommPortIdentifier;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.PropertyConfigurator;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.wpan.TxRequest64;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import com.rapplogic.xbee.util.ByteUtils;

/**
 * An instance of XBeeListenerGui contains the GUI: initialization and display
 */
public class XBeeListenerGui extends javax.swing.JFrame {

	private static final int baud = 9600; //serial comm rate


	private static final String[] addresses = { "1: 0013A200 / 40BF5647", "2: 0013A200 / 40BF56A5",
			"3: 0013A200 / 409179A7", "4: 0013A200 / 4091796F" };

	private static final XBeeAddress64 addr[] = { new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0xbf, 0x56, 0x47),	//long cable
							 					  new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0xbf, 0x56, 0xa5),	//new xbees, small cable
							 					  new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0x91, 0x79, 0xa7),
							 					  new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0x91, 0x79, 0x6f)
												};

	private XBeeAddress64 addr64;
	private XBeeListenerThread xbeeListener;
	
	private int nr = 0; //number received packets
	private int ns = 0;	//number sent packets
	private int ne = 0; //number error packets

	private JLabel packetLabel;
	private JLabel nLabel;

	private JTextArea receiveText;
	private JTextField sendEdit;
	private final static Font titleFont = new Font("Arial", Font.BOLD, 20);
	private final static Font textAreaFont = new Font("Arial", Font.PLAIN, 10);

	private JComboBox serialPortsList;
	private JComboBox addressesList;

	public XBee xbee = new XBee(); //keep as public reference @see XBeeListenerThread.java
	
	
	/* Getters and Setters for packet counters*/
	public int getNumSent() { return ns;}
	public void incNumSent() { ns++; }
	public int getNumRec() {return nr;}
	public void incNumRec() { nr++; }
	public int getNumError() {return ne;}
	public void incNumError() { ne++; }
	public void resetPacketCounters() { ns=0; nr=0; ne=0; }

	/**
	 * Constructor 
	 */
	public XBeeListenerGui() {

		// String path= System.getProperty("user.dir");

		// System.setProperty( "java.library.path", path + "/lib/");
		// System.setProperty("java.library.path", "/Users/sjd227/Desktop/lib");
		// System.out.println(System.getProperty("java.library.path"));

		PropertyConfigurator.configure("./lib/log4j.properties");
		// final XBee xbee = new XBee();

		// short cable:
		// UNCOMMENTED OUT FOR GUI LAYOUT DEV:
		// xbee.open("/dev/cu.usbserial-A5025UEI", 9600);

		// long cable:
		// xbee.open("/dev/cu.usbserial-A603H5XW", 9600);
		// xbee.open("/dev/cu.usbserial-A702LFEL", 9600);
		// JSONObject obj= new JSONObject("{\"name\": \"booster\"}");
		// System.out.println("Before WHILE JSON: " + obj.getString("name"));

		/*
		 * int[] payload = new int[] {'P' }; final ZNetTxRequest request = new
		 * ZNetTxRequest(addr64, payload);
		 */

		// Layout GUI
		JPanel fullPanel = new JPanel(new BorderLayout());
		setContentPane(fullPanel);

		/*-----------------------------Setup XBees Panel----------------------------*/
		JPanel xbeeInitPanel = new JPanel(new BorderLayout());
		JLabel xbeeInitLabel = new JLabel("Setup XBees", JLabel.CENTER);
		xbeeInitLabel.setFont(titleFont);
		xbeeInitPanel.add(xbeeInitLabel, BorderLayout.NORTH);
		
		//XBee Serial Port Label
		JPanel xbeeInitGrid = new JPanel(new GridLayout(2, 2));
		JPanel serialPortPanel = new JPanel(new BorderLayout());
		serialPortPanel.add(new JLabel("GS XBee Serial Port: "), BorderLayout.WEST);

		//Serial port dropdown
		serialPortsList = new JComboBox<String[]>(); //initialize empty dropdown
		updateSerialPortsList();
		serialPortsList.setSelectedIndex(serialPortsList.getItemCount() - 1);

		//Refresh serial ports button
		serialPortPanel.add(serialPortsList, BorderLayout.CENTER);
		JButton refreshPortsBtn = new JButton("Refresh");
		refreshPortsBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateSerialPortsList();
			}
		});
		serialPortPanel.add(refreshPortsBtn, BorderLayout.EAST);
		xbeeInitGrid.add(serialPortPanel);

		//Wireless Address Dropdown
		JPanel addressPanel = new JPanel(new BorderLayout());
		addressPanel.add(new JLabel("Wireless XBee Address (1):"), BorderLayout.WEST);
		addressesList = new JComboBox(addresses);
		addressesList.setSelectedIndex(0);
		addressesList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateAddr();
			}
		});
		addressPanel.add(addressesList, BorderLayout.CENTER);
		xbeeInitGrid.add(addressPanel);
		xbeeInitPanel.add(xbeeInitGrid, BorderLayout.CENTER);

		//Initialize GS XBee Button
		JButton initXBeeButton = new JButton("Initialize GS XBee");
		initXBeeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					initXbee();
					addToReceiveText("Success! Initialized GS XBee :)");
					addToReceiveText("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
							+ System.getProperty("line.separator"));
				} catch (XBeeException e1) {
					ne++;
					addToReceiveText("Error ("
							+ ne
							+ "): Could not connect to XBee :( make sure port isn't being used by another program (including this one)!");
				}
			}
		});
		JPanel xbeeInitButtons = new JPanel(new BorderLayout());
		xbeeInitButtons.add(initXBeeButton, BorderLayout.NORTH);

		//Test Send Button
		JButton testSendBtn = new JButton("Test Send");
		testSendBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendXBeePacket("(Test Packet)");
			}
		});
		xbeeInitButtons.add(testSendBtn, BorderLayout.SOUTH);

		//Add initialize XBee and Test buttons
		xbeeInitPanel.add(xbeeInitButtons, BorderLayout.SOUTH);

		//Send Packet Title and Button
		JPanel sendPacketsPanel = new JPanel(new BorderLayout());
		JLabel sendTitle = new JLabel("Send Packets", JLabel.CENTER);
		sendTitle.setFont(titleFont);
		sendPacketsPanel.add(sendTitle, BorderLayout.NORTH);

		JButton btn = new JButton("Send Data");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// For debugging:
				/*
				 * nr= nr+1; addToReceiveText("Received (" + nr + "): " +
				 * sendEdit.getText(),receiveText);
				 */

				sendXBeePacket();
			}

		});

		sendPacketsPanel.add(btn, BorderLayout.CENTER);
		
		//Send Custom Packet Textbox
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(new JLabel("Send Packet: "), BorderLayout.WEST);
		sendEdit = new JTextField("", 20);
		p2.add(sendEdit, BorderLayout.CENTER);
		// p2.add(new JLabel(""));
		/*
		 * p2.add(new JLabel("Recieved Packet: ")); packetLabel= new JLabel("");
		 * p2.add(packetLabel); p2.add(new JLabel("Count: ")); nLabel= new
		 * JLabel("" + n); p2.add(nLabel);
		 */
		sendPacketsPanel.add(p2, BorderLayout.SOUTH);
		JPanel PContainer = new JPanel(new BorderLayout());
		PContainer.add(xbeeInitPanel, BorderLayout.NORTH);
		PContainer.add(sendPacketsPanel, BorderLayout.CENTER);

		/*----------------------------Received Packets Panel-----------------------------*/
		JPanel receivePanel = new JPanel(new BorderLayout());
		receiveText = new JTextArea(40, 60);
		receiveText.setBackground(Color.white);
		receiveText.setFont(textAreaFont);
		receiveText.setLineWrap(true);
		receiveText.setWrapStyleWord(true);
		receiveText.setEditable(false);
		JScrollPane receiveScrollPlane = new JScrollPane(receiveText);

		JLabel receiveTitle = new JLabel("Received Packets", JLabel.CENTER);
		receiveTitle.setFont(titleFont);
		receivePanel.add(receiveTitle, BorderLayout.NORTH);
		receivePanel.add(receiveScrollPlane, BorderLayout.SOUTH);

		fullPanel.add(PContainer, BorderLayout.WEST);
		fullPanel.add(receivePanel, BorderLayout.CENTER);

		
		//Main window props
		setTitle("XBee Tester");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		pack();
		setVisible(true);

		// Text area stuff...

	}

	/* TODO: function not necessary*/
	public void updateAddr() {
		addr64 = addr[addressesList.getSelectedIndex()];
	}

	public void initXbee() throws XBeeException {

		// get selected serial port...
		String selSerial = (String) serialPortsList.getSelectedItem();

		if (xbee != null && xbee.isConnected()) {
			xbee.close();
			xbeeListener.stopListening();
		}
		
		//TODO: Explain?
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// xbee.open("/dev/cu.usbserial-A5025UEI", 9600);
		System.out.println(selSerial);
		xbee.open(selSerial, baud); //open port
		xbeeListener = new XBeeListenerThread(this); //init a new listener thread
		xbeeListener.start();

		// update addresses of wireless xbees
		updateAddr(); //TODO: isn't address already updated in the dropdown's event handler?

		resetPacketCounters();
	}

	/**
	 * Send a packet to the remote XBee containing user defined text
	 * @TODO: Remove, collect text on actionperformed and invoke single param analog
	 * @void
	 */
	public void sendXBeePacket() {
		String r = sendEdit.getText();
		sendXBeePacket(r);
	}

	/**
	 * Send a packet to remote XBee
	 * @param r		text to send
	 */
	public void sendXBeePacket(String r) {
		try {
			// send a request and wait up to 10 seconds for the response
			int[] payload = new int[r.length()];
			for (int i = 0; i < r.length(); i++) {
				payload[i] = r.charAt(i);
			}
			final ZNetTxRequest request = new ZNetTxRequest(addr64, payload);
			ZNetTxStatusResponse response = (ZNetTxStatusResponse) xbee.sendSynchronous(request, 10000);
			if (response.isSuccess()) {
				// packet was delivered successfully
				// System.out.println("Success!");
				ns++;
				addToReceiveText("Sent (" + ns + "): " + r);
			} else {
				// packet was not delivered
				// System.out.println("Packet was not delivered");
				ne++;
				addToReceiveText("Error (" + ne + "): Packet not delivered - '" + r + "'");
			}
		} catch (XBeeTimeoutException e1) {
			// System.out.println("THERE WAS AN ERROR");
			ne++;
			addToReceiveText("Error (" + ne + "): Packet delivery timed out - '" + r + "'");
			// no response was received in the allotted time

		} catch (XBeeException e1) {
			ne++;
			addToReceiveText("Error (" + ne + "): Packet not delivered b/c of XBee Exception: " + e1.getMessage());
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e) {
			ne++;
			addToReceiveText("Error (" + ne + "): Java Error. Make sure GS XBee is initialized: " + e.getMessage());
		}

	}

	/**
	 * updated the Serial Port List (i.e. after a refresh)
	 * @void
	 */
	public void updateSerialPortsList() {
		ArrayList<String> comboBoxList = new ArrayList<String>();
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();// this line was false
		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				comboBoxList.add(portId.getName());
				// System.out.println(portId.getName());
			} else {
				// System.out.println(portId.getName());
			}
		}

		// update list...
		serialPortsList.removeAllItems();
		for (String s : comboBoxList) {
			serialPortsList.addItem(s);
		}
	}

	/**
	 * Adds text to the Received Packets Box
	 * @param txt			text to add
	 */
	public void addToReceiveText(String txt) {
		receiveText.setText(receiveText.getText() + "- " + txt + System.getProperty("line.separator"));
		receiveText.setCaretPosition(receiveText.getDocument().getLength()); // locks scroll at bottom
	}

	/**
	 * Entry point of the program
	 * @param args
	 * @throws XBeeException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws XBeeException, InterruptedException {

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new XBeeListenerGui().setVisible(true);
			}
		});
	}
}