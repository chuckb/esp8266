import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Set;

import com.chuckb.embedded.AccessPoint;
import com.chuckb.embedded.ESP8266;
import com.chuckb.embedded.ProtocolException;
import com.chuckb.embedded.ResponseFailedException;
import com.chuckb.embedded.ResponseTimeoutException;
import com.fazecast.jSerialComm.*;

public class Main {
  public static void main(String[] args) throws IOException, ProtocolException, ResponseFailedException, ResponseTimeoutException {
    String port = "COM5";                 // Comm port connected to the ESP8266 (on Windows, COMx...use mode command to find)
    String SSID = "ROBOT";                // When testing as a station, this is the SSID of the access point to connect the client to
    String password = "";                 // When testing as a station, this is the password of the access point
    String remoteIP = "192.168.5.230";    // For the UDP test, this is the IP of the remote end
    int remotePort = 1001;                // This is the remote port to open up for the UDP test
    int localPort = 2233;                 // This is the local port to open up on the ESP8266 for the UDP test

    SerialPort comPort = SerialPort.getCommPort(port);
    comPort.setBaudRate(9600);
    comPort.openPort();
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
          System.out.println("Perform UDP tests...");
          esp8266.joinAccessPoint(SSID, password, 6000);
          System.out.println("Joined access point " + SSID);
          System.out.println(esp8266.getIPAddress());
          esp8266.startUDPClient(remoteIP, remotePort, localPort, ESP8266.UDPPeerModeEnum.USEDEFINEDREMOTE);
          System.out.println("On a host connected to the " + SSID + " access point, run `nc -u -l " + remotePort + "`");
          System.out.println("Press enter when ready...");
          System.console().readLine();
          byte[] knockBytes = "Knock, knock?".getBytes(Charset.forName("US-ASCII"));
          esp8266.send(ByteBuffer.wrap(knockBytes), 300);
          System.out.println("Knock, knock?");
          System.out.println("After pressing enter, go to host and enter `Who there\n`...");
          System.console().readLine();
          System.out.println("Waiting for who there...");
          ByteBuffer whoThere = ByteBuffer.allocate(20);
          esp8266.receive(whoThere, 10000);
          whoThere.flip();
          try {
            while(true) {
              System.out.print((char)whoThere.get());
            }
          } catch (Exception e) {
            esp8266.closeIPClient();
            System.out.println("UDP client closed.");
          }
        }
      }
    } finally {
      comPort.closePort();
    }
  }
}