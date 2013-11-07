import java.net.*; 
import java.util.*;  
import java.nio.*;

public class EchoClient { 

public static byte[] convertToFourBytes(int value) {

    byte[] r = new byte[4];
    r[0] = (byte)(value >>> 24);
    r[1] = (byte)(value >>> 16);
    r[2] = (byte)(value >>> 8);
    r[3] = (byte)value;
    return r;
}

public static byte[] convertToTwoBytes(int i){
  byte[] result = new byte[2];

  result[0] = (byte) (i >> 8);
  result[1] = (byte) (i /*>> 0*/);
  // System.out.println("-------");
  // System.out.println(i);
  // System.out.println(result[1]);
  // System.out.println(result[0]);
  // System.out.println("-----");
  // System.out.println(fromByteArray(result));
  // System.out.println("-------");
  // System.out.println(bb.getInt());
  return result;
}


public static byte[] concat(byte[] first, byte[] second) {
  byte[] result = Arrays.copyOf(first, first.length + second.length);
  System.arraycopy(second, 0, result, first.length, second.length);
  return result;
}

static short shortFromByteArray(byte[] bytes) {
     return ByteBuffer.wrap(bytes).getShort();
}
static int intFromByteArray(byte[] bytes) {
     return ByteBuffer.wrap(bytes).getInt();
}

public static void main( String args[] ) throws Exception { 
  byte[] header = new byte[20];

  byte[] port1 = convertToTwoBytes(5555);
  byte[] port2 = convertToTwoBytes(4000);
  byte[] sequenceNumber = convertToFourBytes(111111);
  byte[] ack = convertToFourBytes(123456);
  byte[] flags = convertToTwoBytes(1);
  byte[] recWindow = convertToTwoBytes(1);
  byte[] checksum = convertToTwoBytes(1);
  byte[] urgent = convertToTwoBytes(1);

  header = concat(port1, port2);
  header = concat(header, sequenceNumber);
  header = concat(header, ack);
  header = concat(header, flags);
  header = concat(header, recWindow);
  header = concat(header, checksum);
  header = concat(header, urgent);
  System.out.println(header.length);

  // byte[] bothTogether = concat(port1, port2);
  byte[] hopefullyPort1 = new byte[2];
  byte[] hopefullyPort2 = new byte[2];
  byte[] hopefullyAck = new byte[4];
  byte[] hopefullyS = new byte[4];

  for (int i = 0; i < 4; i++){
    hopefullyS[i] = header[i+4];
  }
  for (int i = 0; i < 2; i++){
    hopefullyPort1[i] = header[i];
  }

  for (int i = 0; i < 2; i++){
    hopefullyPort2[i] = header[i+2];
  }

  System.out.println(shortFromByteArray(port1));
  System.out.println(shortFromByteArray(hopefullyPort1));
  System.out.println("----");
  System.out.println(shortFromByteArray(port2));
  System.out.println(shortFromByteArray(hopefullyPort2));
  System.out.println("----");
  System.out.println(intFromByteArray(sequenceNumber));
  System.out.println(intFromByteArray(hopefullyS));
  // convertToBytes(10000);
  // convertToBytes(16);



  // InetAddress add = InetAddress.getByName("localhost");   
  // System.out.println("Starting the client "+args[0] + "," + args[1]); 
  // DatagramSocket dsock = new DatagramSocket(Integer.parseInt(args[1])); 
  // System.out.println("Client socket = " + dsock.getPort()); 
  // // String message1 = "This is client calling"; 
  // byte arr[] = message1.getBytes( ); 
  // // byte header[] = 
  // DatagramPacket dpack = new DatagramPacket(arr, arr.length, add, Integer.parseInt(args[0])); 
  // while(true){
  //  // System.out.println("Sending the packet "); 
  //  dpack = new DatagramPacket(arr, arr.length, add, Integer.parseInt(args[0]));
  //  System.out.println("Sending the packet to " + dpack.getPort());
  //  dsock.send(dpack); // send the packet 
  //  Date sendTime = new Date( ); // note the time of sending the message   
  //  dsock.receive(dpack); // receive the packet 
  //  String message2 = new String(dpack.getData()); 
  //  System.out.println(message2);
  //  Date receiveTime = new Date( ); // note the time of receiving the message 
  //  System.out.println((receiveTime.getTime( ) - sendTime.getTime( )) + " milliseconds echo time for " + message2); 
  //  Thread.sleep(1000);
  // } 
 } 
} 
