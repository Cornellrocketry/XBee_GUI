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

public class Main {

	private static final int baud = 9600;

	static String selSerial;
	static int[] selAddr = new int[8];

	static String[] addresses = { "1: 0013A200 / 40BF5647",
			"2: 0013A200 / 40BF56A5", "3: 0013A200 / 409179A7",
			"4: 0013A200 / 4091796F" };

	static int[][] addr = { { 0, 0x13, 0xa2, 0, 0x40, 0xbf, 0x56, 0x47 },
			{ 0, 0x13, 0xa2, 0, 0x40, 0xbf, 0x56, 0xa5 },
			{ 0, 0x13, 0xa2, 0, 0x40, 0x91, 0x79, 0xa7 },
			{ 0, 0x13, 0xa2, 0, 0x40, 0x91, 0x79, 0x6f } };

	// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40,
	// 0x91, 0x79, 0xa7);
	// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40,
	// 0x91, 0x79, 0x6f);

	// new xbees:
	// small cable:
	// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40,
	// 0xbf, 0x56, 0xa5);

	// long cable:
	// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40,
	// 0xbf, 0x56, 0x47);

	static XBeeAddress64 addr64;
	static XBeeListenerThread xbeeListener;
	static int nr;
	static int ns;
	static int ne;

	static JLabel packetLabel;
	static JLabel nLabel;

	static JTextArea receiveText;
	static JTextField sendEdit;
	int[] payload;
	private final static Font titleFont = new Font("Arial", Font.BOLD, 20);
	private final static Font textAreaFont = new Font("Arial", Font.PLAIN, 10);

	static JComboBox serialPortsList;
	static JComboBox addressesList;

	static XBee xbee;

	public static void updateAddr() {
		int idx = addressesList.getSelectedIndex();
		selAddr = addr[idx];
		addr64 = new XBeeAddress64(selAddr[0], selAddr[1], selAddr[2],
				selAddr[3], selAddr[4], selAddr[5], selAddr[6], selAddr[7]);
	}

	public static void initXbee() throws XBeeException {

		// get selected serial port...
		selSerial = (String) serialPortsList.getSelectedItem();

		if (xbee != null && xbee.isConnected()) {
			xbee.close();
			xbeeListener.keepListening = false;
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		xbee = new XBee();
		// xbee.open("/dev/cu.usbserial-A5025UEI", 9600);
		System.out.println(selSerial);
		xbee.open(selSerial, baud);
		xbeeListener = new XBeeListenerThread();
		xbeeListener.start();

		// update addresses of wireless xbees...
		updateAddr();

		nr = 0;
		ns = 0;
		ne = 0;
	}

	public static void sendXBeePacket() {
		String r = sendEdit.getText();
		sendXBeePacket(r);
	}

	public static void sendXBeePacket(String r) {
		try {
			// send a request and wait up to 10 seconds for the response
			int[] payload = new int[r.length()];
			for (int i = 0; i < r.length(); i++) {
				payload[i] = r.charAt(i);
			}
			final ZNetTxRequest request = new ZNetTxRequest(addr64, payload);
			ZNetTxStatusResponse response = (ZNetTxStatusResponse) xbee
					.sendSynchronous(request, 10000);
			if (response.isSuccess()) {
				// packet was delivered successfully
				// System.out.println("Success!");
				ns = ns + 1;
				addToReceiveText("Sent (" + ns + "): " + r);
			} else {
				// packet was not delivered
				// System.out.println("Packet was not delivered");
				ne++;
				addToReceiveText("Error (" + ne + "): Packet not delivered - '"
						+ r + "'");
			}
		} catch (XBeeTimeoutException e1) {
			// System.out.println("THERE WAS AN ERROR");
			ne++;
			addToReceiveText("Error (" + ne
					+ "): Packet delivery timed out - '" + r + "'");
			// no response was received in the allotted time

		} catch (XBeeException e1) {
			ne++;
			addToReceiveText("Error (" + ne
					+ "): Packet not delivered b/c of XBee Exception: "
					+ e1.getMessage());
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e) {
			ne++;
			addToReceiveText("Error (" + ne
					+ "): Java Error. Make sure GS XBee is initialized: "
					+ e.getMessage());
		}

	}

	public static void updateSerialPortsList() {
		ArrayList<String> comboBoxList = new ArrayList<String>();
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();// this
																		// line
																		// was
																		// false
		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList
					.nextElement();
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

	public static void main(String[] args) throws XBeeException,
			InterruptedException {

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

		// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0,
		// 0x40, 0x91, 0x79, 0xa7);
		// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0,
		// 0x40, 0x91, 0x79, 0x6f);

		// new xbees:
		// small cable:
		// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0,
		// 0x40, 0xbf, 0x56, 0xa5);

		// long cable:
		// final XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0,
		// 0x40, 0xbf, 0x56, 0x47);

		/*
		 * int[] payload = new int[] {'P' }; final ZNetTxRequest request = new
		 * ZNetTxRequest(addr64, payload);
		 */

		nr = 0;
		ns = 0;
		ne = 0;

		// Layout GUI...

		JFrame f = new JFrame("XBee Tester");
		JPanel fullPanel = new JPanel(new BorderLayout());
		f.setContentPane(fullPanel);

		JPanel xbeeInitPanel = new JPanel(new BorderLayout());
		JLabel xbeeInitLabel = new JLabel("Setup XBees", JLabel.CENTER);
		xbeeInitLabel.setFont(titleFont);
		xbeeInitPanel.add(xbeeInitLabel, BorderLayout.NORTH);

		JPanel xbeeInitGrid = new JPanel(new GridLayout(2, 2));
		JPanel serialPortPanel = new JPanel(new BorderLayout());
		serialPortPanel.add(new JLabel("GS XBee Serial Port: "),
				BorderLayout.WEST);

		// Initializing xbee...
		String[] list = { "" };
		serialPortsList = new JComboBox(list);
		updateSerialPortsList();
		serialPortsList.setSelectedIndex(serialPortsList.getItemCount() - 1);

		serialPortPanel.add(serialPortsList, BorderLayout.CENTER);
		JButton refreshPortsBtn = new JButton("Refresh");
		refreshPortsBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateSerialPortsList();
			}
		});
		serialPortPanel.add(refreshPortsBtn, BorderLayout.EAST);
		xbeeInitGrid.add(serialPortPanel);

		JPanel addressPanel = new JPanel(new BorderLayout());
		addressPanel.add(new JLabel("Wireless XBee Address (1):"),
				BorderLayout.WEST);
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

		JButton testSendBtn = new JButton("Test Send");
		testSendBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendXBeePacket("(Test Packet)");
			}
		});
		xbeeInitButtons.add(testSendBtn, BorderLayout.SOUTH);

		xbeeInitPanel.add(xbeeInitButtons, BorderLayout.SOUTH);

		JPanel p = new JPanel(new BorderLayout());

		JLabel sendTitle = new JLabel("Send Packets", JLabel.CENTER);
		sendTitle.setFont(titleFont);
		p.add(sendTitle, BorderLayout.NORTH);

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

		p.add(btn, BorderLayout.CENTER);
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
		p.add(p2, BorderLayout.SOUTH);
		JPanel PContainer = new JPanel(new BorderLayout());
		PContainer.add(xbeeInitPanel, BorderLayout.NORTH);
		PContainer.add(p, BorderLayout.CENTER);

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

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setResizable(false);
		f.pack();
		f.setVisible(true);

		// Text area stuff...

	}

	public static void addToReceiveText(String txt) {
		receiveText.setText(receiveText.getText() + "- " + txt
				+ System.getProperty("line.separator"));
		receiveText.setCaretPosition(receiveText.getDocument().getLength()); // locks
																				// scroll
																				// at
																				// bottom
	}

}