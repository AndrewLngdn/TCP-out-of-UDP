import java.net.*; 
import java.util.*;   
import java.nio.*;
import java.util.Random;


public class Receiver { 

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


  public static byte[] makeHeader(int sPort, int dPort, int seq_num){
    byte[] header = new byte[20];

    byte[] sourcePort = convertToTwoBytes(sPort);
    byte[] destinationPort = convertToTwoBytes(dPort);
    byte[] sequenceNumber = convertToFourBytes(seq_num);
    byte[] ack = convertToFourBytes(seq_num);
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

  static short shortFromByteArray(byte[] bytes) {
     return ByteBuffer.wrap(bytes).getShort();
  }

  static int intFromByteArray(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getInt();
  }

  public static byte[] concat(byte[] first, byte[] second) {
    byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  public static int seq_num;
  public static int ack_num;


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

    short source_port = shortFromByteArray(source_port_a);
    short dest_port = shortFromByteArray(dest_port_a);
    seq_num = intFromByteArray(seq_num_a);
    ack_num = intFromByteArray(ack_num_a);

    System.out.println("seq_num: " + seq_num);
    System.out.println("ack_num: " + ack_num);
  }



  public static void main( String args[]) throws Exception { 

    if (args.length != 5){
      System.out.println("java Receiver [filename] [listening_port] [remote_ip] [remote_port] [log_filename]");
      System.exit(1);
    }


    String filename = args[0];
    int listening_port = Integer.parseInt(args[1]);
    String remote_ip = args[2];
    int remote_port= Integer.parseInt(args[3]);
    String log_filename = args[4];

    DatagramSocket dsock = new DatagramSocket(listening_port); 
    System.out.println("Starting server " + dsock.getPort());   

    System.out.println("Started");   

    int expected_seq_num = 1;


    while(true) { 
      byte[] buffer = new byte[576]; 
      DatagramPacket rpack = new DatagramPacket(buffer, buffer.length);

      dsock.receive(rpack);  // need checksum
      byte[] packet = rpack.getData();
      decodeHeader(packet);

      if (seq_num == expected_seq_num){ // also do checksum
        System.out.println("got expected packet");
        //send ack
        byte[] ack_pack_data = makeHeader(listening_port, remote_port, seq_num); 
        
        expected_seq_num++;
        InetAddress remote_addr = InetAddress.getByName(remote_ip);
        
        DatagramPacket spack = new DatagramPacket(ack_pack_data, ack_pack_data.length, remote_addr, remote_port); 
        // spack.setData("hello!".getBytes());
        dsock.send(spack); 
      }
    } 
  } 
}
