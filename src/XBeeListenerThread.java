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

	public String data = "";
	public boolean keepListening;
	static boolean receiving = false;

	public XBeeListenerThread() {
		keepListening = true;
	}

	@Override
	public void run() {
		while (keepListening) {
			try {
				XBeeResponse response = Main.xbee.getResponse();
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
							Main.ne++;
							Main.addToReceiveText("Error ("
									+ Main.ne
									+ "): New packet before last data stream ended...");
							Main.nr++;
							Main.addToReceiveText("Incomplete Received ("
									+ Main.nr + "): " + data);
							Main.nr++;
							Main.addToReceiveText("New Received (" + Main.nr
									+ "): " + packet);
						}
						receiving = true;
					} else if (!receiving) {
						Main.nr++;
						Main.addToReceiveText("Received (" + Main.nr + "): "
								+ packet);
					} else {
						// System.out.println("recieved...!");
						// System.out.println(packet);
						data += packet;
						if (packet.charAt(packet.length() - 1) == '>') {
							// end of data stream reached--> return data and
							// reset...
							// System.out.println(data);
							Main.nr++;
							Main.addToReceiveText("Received (" + Main.nr
									+ "): " + data);
							receiving = false;
							data = "";

						}
					}
				}
			} catch (XBeeTimeoutException e) {
				// we timed out without a response
			} catch (XBeeException e) {
				Main.ne++;
				Main.addToReceiveText("Error (" + Main.ne + "): XBee Problem: "
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

}
