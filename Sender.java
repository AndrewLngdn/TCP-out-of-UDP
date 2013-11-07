import java.net.*; 
import java.util.*;  
import java.nio.*;
import java.io.*;

public class Sender { 

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

public static byte[] makeHeader(int sPort, int dPort, int seq_num){
  byte[] header = new byte[20];

  byte[] sourcePort = convertToTwoBytes(sPort);
  byte[] destinationPort = convertToTwoBytes(dPort);
  byte[] sequenceNumber = convertToFourBytes(seq_num);
  byte[] ack = convertToFourBytes(123456);
  byte[] flags = convertToTwoBytes(1);
  byte[] recWindow = convertToTwoBytes(1);
  byte[] checksum = convertToTwoBytes(1);
  byte[] urgent = convertToTwoBytes(1);

  header = concat(sourcePort, destinationPort);
  header = concat(header, sequenceNumber);
  header = concat(header, ack);
  header = concat(header, flags);
  header = concat(header, recWindow);
  header = concat(header, checksum);
  header = concat(header, urgent);

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

  System.out.println(shortFromByteArray(sourcePort));
  System.out.println(shortFromByteArray(hopefullyPort1));
  System.out.println("----");
  System.out.println(shortFromByteArray(destinationPort));
  System.out.println(shortFromByteArray(hopefullyPort2));
  System.out.println("----");
  System.out.println(intFromByteArray(sequenceNumber));
  System.out.println(intFromByteArray(hopefullyS));

  return header;
}

public static ArrayList<byte[]> packets = new ArrayList<byte[]>();

public static void main( String args[] ) throws Exception { 
   

  if (args.length != 6){
    System.out.println("java Sender [filename] [remote_IP] [remote_port] [ack_port_number] [window_size] [log_filename]");
    System.exit(1);
  } 

  String filename = args[0];
  String remote_ip = args[1];
  int remote_port = Integer.parseInt(args[2]);
  int ack_port = Integer.parseInt(args[3]);
  int window_size = Integer.parseInt(args[4]); // packets
  String log_filename = args[5];



  int timeout = 50; // milliseconds
  int base = 1; 
  int nextseqnum = 1;



  BufferedReader br = null;

  // try {

  //   String line;
  //   br = new BufferedReader(new FileReader(filename));

  //   while ((line = br.readLine()) != null) {
  //     System.out.println(line);
  //   }

  // } catch (IOException e) {
  //   e.printStackTrace();
  // }

  InetAddress remote_addr = InetAddress.getByName(remote_ip);   
  // System.out.println("Starting the client "+args[0] + "," + args[1]); 
  DatagramSocket dsock = new DatagramSocket(ack_port); 
  System.out.println("Client socket = " + dsock.getPort()); 
  String message1 = "DATA FOR SERVER"; 
  byte[] data = message1.getBytes(); 


  while(true){
    byte[] header = makeHeader(ack_port, remote_port, nextseqnum); // get things from string args
    System.out.println("header length:" + header.length);

   // System.out.println("Sending the packet "); 
   // dpack = new DatagramPacket(arr, arr.length, add, Integer.parseInt(args[0]));
   // System.out.println("Sending the packet to " + remote_addr + ":" + dpack.getPort());
    DatagramPacket dpack = new DatagramPacket(header, header.length, remote_addr, remote_port); 

    System.out.println("data length:" + dpack.getLength());

    dsock.send(dpack); // send the packet 
    nextseqnum++;
    System.out.println("nextseqnum: " + nextseqnum);

    Date sendTime = new Date(); // note the time of sending the message   

    dsock.receive(dpack); // receive the ack

    // String message2 = new String(dpack.getData()); 
    // System.out.println(message2);
    Date receiveTime = new Date( ); // note the time of receiving the message 
    // System.out.println((receiveTime.getTime( ) - sendTime.getTime( )) + " milliseconds echo time for " + message2); 
    Thread.sleep(2000);
  } 
 } 
} 
