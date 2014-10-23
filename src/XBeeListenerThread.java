// License: GPL. For details, see LICENSE file.

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import com.rapplogic.xbee.util.ByteUtils;

public class XBeeListenerThread extends Thread {

	private String data = "";
	private boolean keepListening;
	static boolean receiving = false;
	private XBeeListenerGui mainWindow;

	public XBeeListenerThread(XBeeListenerGui gui) {
		mainWindow = gui;
		keepListening = true;
	}
	
	public void stopListening() { keepListening = false; }

	@Override
	public void run() {
		while (keepListening) {
			try {
				XBeeResponse response = mainWindow.xbee.getResponse();
				// System.out.println(response.getApiId().toString());
				// System.out.println(response.getClass().toString());
				if (response.getApiId() == ApiId.ZNET_RX_RESPONSE) {

					ZNetRxResponse ioSample = (ZNetRxResponse) response;

					String packet = ByteUtils.toString(ioSample.getData());
					// System.out.println("Recieved Data: " + packet);
					// nLabel.setText("" + nr);

					if (packet.charAt(0) == '<') {
						// long incoming packet, formated "<[packet]>"
						System.out.println("start!");
						if (receiving) {
							// System.out.println("ERROR: new packet before last data stream ended");
							mainWindow.incNumError();
							mainWindow.addToReceiveText("Error ("
									+ mainWindow.getNumError()
									+ "): New packet before last data stream ended...");
							mainWindow.incNumRec();
							mainWindow.addToReceiveText("Incomplete Received ("
									+ mainWindow.getNumRec() + "): " + data);
							mainWindow.incNumRec();
							mainWindow.addToReceiveText("New Received (" + mainWindow.getNumRec()
									+ "): " + packet);
						}
						receiving = true;
					} else if (!receiving) {
						mainWindow.incNumRec();
						mainWindow.addToReceiveText("Received (" + mainWindow.getNumRec() + "): "
								+ packet);
					} else {
						// System.out.println("recieved...!");
						// System.out.println(packet);
						data += packet;
						if (packet.charAt(packet.length() - 1) == '>') {
							// end of data stream reached--> return data and
							// reset...
							// System.out.println(data);
							mainWindow.incNumRec();
							mainWindow.addToReceiveText("Received (" + mainWindow.getNumRec()
									+ "): " + data);
							receiving = false;
							data = "";

						}
					}
				}
			} catch (XBeeTimeoutException e) {
				// we timed out without a response
			} catch (XBeeException e) {
				mainWindow.incNumError();
				mainWindow.addToReceiveText("Error (" + mainWindow.getNumError() + "): XBee Problem: "
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

}
