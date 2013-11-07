import java.net.*; 
import java.util.*;  
import java.nio.*;
import java.io.*;

public class Sender implements Runnable { 

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
public static int nextseqnum = 1;
public static int base = 1;
public static int timeout = 50;
public static String filename;
public static String remote_ip;
public static int remote_port;
public static int ack_port;
public static int window_size = 1;
public static String log_filename;

public static DatagramSocket dsock; 


public static void main( String args[] ) throws Exception { 

  boolean execute = true;

  if (args.length != 6){
    System.out.println("java Sender [filename] [remote_IP] [remote_port] [ack_port_number] [window_size] [log_filename]");
    System.exit(1);
  } 

  filename = args[0];
  remote_ip = args[1];
  remote_port = Integer.parseInt(args[2]);
  ack_port = Integer.parseInt(args[3]);
  window_size = Integer.parseInt(args[4]); // packets
  log_filename = args[5];

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

  // loop 1 
  try {
    dsock = new DatagramSocket(ack_port);
    new Thread(new Sender()).start();

    // listen for acks
    while (execute){
      byte[] buffer = new byte[576];

      DatagramPacket dpack = new DatagramPacket(buffer, buffer.length);

      dsock.receive(dpack); // receive the ack
      base++; // base = getacknum(recpck)+1

      System.out.println("recieved packet: " + dpack.getData());
      //wait for acks
    }

  } catch (Exception e){
    System.err.println("IOEx: " + e);
  }







  
 } 

  // make & send packets here

  public void run(){
      try {
        while(true){
          if (nextseqnum < base+window_size){
            InetAddress remote_addr = InetAddress.getByName(remote_ip);   
          
            byte[] header = makeHeader(ack_port, remote_port, nextseqnum); // get things from string args
            System.out.println("header length:" + header.length);

            DatagramPacket dpack = new DatagramPacket(header, header.length, remote_addr, remote_port); 

            System.out.println("data length:" + dpack.getLength());

            dsock.send(dpack); // send the packet 
            System.out.println("nextseqnum: " + nextseqnum);

            Date sendTime = new Date(); // note the time of sending the message   

            Date receiveTime = new Date( ); // note the time of receiving the message 
            Thread.sleep(2000);

            // make/send packet
            if (base == nextseqnum){
              //start_timer
            }

            nextseqnum++;
          }
        }
        
      } catch (Exception e){
        System.err.println("ex: " + e);
      }

  }
} 
