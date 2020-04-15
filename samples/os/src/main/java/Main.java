import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.chuckb.embedded.AccessPoint;
import com.chuckb.embedded.ESP8266;
import com.chuckb.embedded.ProtocolException;
import com.chuckb.embedded.ResponseFailedException;
import com.chuckb.embedded.ResponseTimeoutException;
import com.fazecast.jSerialComm.*;

/**
 * System test for ESP8266 class. Exercise methods using a Java serial class that supports
 * Windows, Mac, and Linux. Output from this test will print to the system console.
 * Input will come from a console if it is wired up to a command line, like when running directly from VSCode
 * for example, or from Java directly. Running from gradle, however, will cause a swing dialog
 * to display for prompting.
 * 
 * <p>Establish a connection with a serial dongle from your host to the ESP8266.</p>
 */
public class Main {
  public static String getInput(String prompt) {
    // This little workaround deals with the fact that gradle does not expose the sysin console.
    // And so a swing dialog is presented as a workaround to solicit user prompting.
    if (System.console() == null) {
      JOptionPane.showMessageDialog(
        ((Supplier<JDialog>) () -> {
          final JDialog dialog = new JDialog();
          dialog.setAlwaysOnTop(true); 
          dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          return dialog;
        }).get(), prompt);
      return "";
    } else {
      return System.console().readLine();
    }
  }

  public static void main(String[] args) throws IOException, ProtocolException, ResponseFailedException, ResponseTimeoutException {
    String port = "COM5";                 // Comm port connected to the ESP8266 (on Windows, COMx...use mode command to find)
    String SSID = "ROBOT";                // When testing as a station, this is the SSID of the access point to connect the client to
    String password = "";                 // When testing as a station, this is the password of the access point
    String remoteIP = "192.168.5.230";    // For the communication tests, this is the IP of the remote end
    int remotePort = 1001;                // This is the remote port to open up for the tests
    int localPort = 2233;                 // This is the local port to open up on the ESP8266 for the UDP test

    SerialPort comPort = SerialPort.getCommPort(port);
    comPort.setBaudRate(9600);
    comPort.openPort();
    System.out.println("Comm port " + port + " opened.");
    try {
      comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 200);
      try(InputStream inputStream = comPort.getInputStream()) {
        try (OutputStream outputStream = comPort.getOutputStream()) {
          ESP8266 esp8266 = new ESP8266(inputStream, outputStream);
          System.out.println(esp8266.isReady());
          System.out.println(esp8266.getFirmwareVersion());
          esp8266.setWifiMode(ESP8266.WifiModeEnum.STATION);
          System.out.println("Set to station mode.");
          esp8266.restart();
          System.out.println("Restarted module.");
          Set<AccessPoint> accessPoints = esp8266.getAccessPoints();
          accessPoints.forEach(accessPoint -> {
            System.out.print(accessPoint.encryption + " ");
            System.out.print(accessPoint.SSID + " ");
            System.out.print(accessPoint.RSSI + " ");
            System.out.print(accessPoint.macAddress + " ");
            System.out.print(accessPoint.channel + "\r\n");
          });
          System.out.println(esp8266.getWifiMode());
          esp8266.setWifiMode(ESP8266.WifiModeEnum.ACCESSPOINT);
          System.out.println("Set to access point mode.");
          esp8266.restart();
          System.out.println("Restarted module.");
          System.out.println(esp8266.getWifiMode());
          System.out.println(esp8266.getIPAddress());
          esp8266.setMuxMode(true);
          System.out.println("Turned on mux mode.");
          esp8266.setWifiMode(ESP8266.WifiModeEnum.STATION);
          System.out.println("Set to station mode.");
          esp8266.restart();
          System.out.println("Restarted module.");
          esp8266.joinAccessPoint(SSID, password, 6000);
          System.out.println("Joined access point " + SSID);
          System.out.println("My address: " + esp8266.getIPAddress());
          System.out.println("Perform UDP tests...");
          System.out.println("On a host connected to the " + SSID + " access point, run `nc -u -l " + remotePort + "`");
          getInput("Press enter when ready...");
          esp8266.startUDPClient(remoteIP, remotePort, localPort, ESP8266.UDPPeerModeEnum.USEDEFINEDREMOTE);
          communicationTest(esp8266);
          esp8266.restart();                                // This should not be needed, but there appears to be a bug clearing the UDP client settings. (Fixed in 019 firmware evidently)
          System.out.println("Restarted module.");
          System.out.println("Perform TCP tests...");
          System.out.println("On a host connected to the " + SSID + " access point, run `nc -l " + remotePort + "`");
          getInput("Press enter when ready...");
          esp8266.startTCPClient(remoteIP, remotePort, 500);
          communicationTest(esp8266);
        }
      }
    } finally {
      comPort.closePort();
      System.out.println("Comm port " + port + " closed.");
    }
    System.out.println("Done!");
    System.exit(0);
  }

  public static void communicationTest(ESP8266 esp8266) throws IOException, ResponseFailedException, ResponseTimeoutException, ProtocolException {
    byte[] knockBytes = "Knock, knock?".getBytes(Charset.forName("US-ASCII"));
    esp8266.send(ByteBuffer.wrap(knockBytes), 300);
    System.out.println("Knock, knock?");
    getInput("After pressing enter, go to host and enter `Who there<return>`...");
    System.out.println("Waiting for Who there...");
    ByteBuffer whoThere = ByteBuffer.allocate(20);
    esp8266.receive(whoThere, 10000);
    whoThere.flip();
    String response = "";
    try {
      while(true) {
        char character = (char)whoThere.get();
        System.out.print(character);
        response += character;
      }
    } catch (BufferUnderflowException e) {
      // This is expected when byte buffer is emptied
    } finally {
      esp8266.closeIPClient();
      System.out.println("Client closed.");
    }
    if (!response.trim().equals("Who there")) {
      throw new ProtocolException("Response not received as expected");
    }
  } 
}