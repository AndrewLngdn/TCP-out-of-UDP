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

  public static void decodeHeader(byte[] packet){
    System.out.println("decoding header");

    byte[] header = Arrays.copyOfRange(packet, 0, 20);
    byte[] source_port_a = Arrays.copyOfRange(header, 0, 2);
    byte[] dest_port_a = Arrays.copyOfRange(header, 2, 4);
    byte[] seq_num_a = Arrays.copyOfRange(header, 4, 8);
    byte[] ack_num_a = Arrays.copyOfRange(header, 8, 12);
    byte[] header_len = Arrays.copyOfRange(header, 12, 13); // always 20
    byte[] flags = Arrays.copyOfRange(header, 13, 14);
    byte[] rec_window = Arrays.copyOfRange(header, 14, 16);
    byte[] chksum = Arrays.copyOfRange(header, 16, 18);
    byte[] urg = Arrays.copyOfRange(header, 18, 20);

    received_ack = intFromByteArray(ack_num_a);
    int seq = intFromByteArray(seq_num_a);
    System.out.println("received ack   " + received_ack);
    System.out.println("seq num   " + seq);

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

    return header;
  }

  public static ArrayList<byte[]> packets = new ArrayList<byte[]>();
  public static int nextseqnum = 0;
  public static int base = 0;
  public static int timeout = 50;
  public static String filename;
  public static String remote_ip;
  public static int remote_port;
  public static int ack_port;
  public static int window_size = 1;
  public static String log_filename;
  public static int received_ack = 0;
  public static byte[] file_bytes;
  public static DatagramSocket dsock; 
  public static InetAddress remote_addr;

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
    remote_addr = InetAddress.getByName(remote_ip); 


    ///// make packets here!
    FileInputStream fileInputStream = null;
    File file = new File(filename);
    file_bytes = new byte[(int)file.length()];

    try {

      fileInputStream = new FileInputStream(file);
      fileInputStream.read(file_bytes);
      fileInputStream.close();

      int numberOfPacketsNeeded = file_bytes.length/576 + 1;
      System.out.println(numberOfPacketsNeeded);

      // byte[] packet = make_pkt(nextseqnum, file_bytes, ack_port, remote_port);

      // for (int i = 0; i < numberOfPacketsNeeded; i++){
      //   packets.add(make_pkt(i,file_bytes, ack_port, remote_port));
      // }
      // System.out.println(packets.size());


    } catch (IOException e) {
      e.printStackTrace();
    }

    // loop 1 
    try {

      dsock = new DatagramSocket(ack_port);
      new Thread(new Sender()).start();

      // listen for acks
      while (execute){
        byte[] buffer = new byte[576];

        DatagramPacket dpack = new DatagramPacket(buffer, buffer.length);

        dsock.receive(dpack); // receive the ack
        decodeHeader(dpack.getData());

        base = received_ack + 1; 

        System.out.println("recieved packet: " + dpack.getData());
      }

    } catch (Exception e){
      System.err.println("IOEx: " + e);
    }
    
  } 

  // make & send packets here

  public static byte[] make_pkt(int nextseqnum, byte[] file_bytes, int ack_port, int remote_port){
    byte[] header = makeHeader(ack_port, remote_port, nextseqnum); // get things from string args
    int data_start = nextseqnum * 556;
    int data_end = Math.min((nextseqnum + 1)*556 + 1, file_bytes.length);

    if (data_start > data_end){
      System.out.println("finished sending");
      System.exit(0);
    }

    byte[] packet_data = Arrays.copyOfRange(file_bytes, data_start, data_end);
    // System.out.println(packet_data.length);
    byte[] packet = concat(header, packet_data);
    packets.add(packet);

    return packet;
  }

  public static long timer;
  public void run(){
      try {
        while(true){

          // check for timeout
            long elapsedTime = (new Date()).getTime() - timer;
            if (elapsedTime > 3000){
              timer = (new Date()).getTime();
              for (int i = 0; i < packets.size(); i++){
                byte[] packet = packets.get(i);
                DatagramPacket dpack = new DatagramPacket(packet, packet.length, remote_addr, remote_port); 
                dsock.send(dpack);
              }
              System.out.println("elapsed Time is too long");
            }

          if (nextseqnum < base+window_size){
          
            // make packet here
            byte[] packet = make_pkt(nextseqnum, file_bytes, ack_port, remote_port);

            DatagramPacket dpack = new DatagramPacket(packet, packet.length, remote_addr, remote_port); 

            dsock.send(dpack); // send the packet 
            System.out.println("nextseqnum: " + nextseqnum);
            Date sendTime = new Date(); // note the time of sending the message   
            Date receiveTime = new Date( ); // note the time of receiving the message 
            Thread.sleep(1000);

            // make/send packet
            if (base == nextseqnum){
              System.out.println("setting timer");
              //start_timer
              timer = (new Date()).getTime();
            }

            nextseqnum++;
          }
        }
        
      } catch (Exception e){
        System.err.println("Eception: ");
        e.printStackTrace();
      }

  }
} 
