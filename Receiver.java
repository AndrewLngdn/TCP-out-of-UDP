import java.net.*; 
import java.util.*;   
import java.nio.*;


public class Receiver { 

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
      int ack_num = intFromByteArray(ack_num_a);

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

    int expectedSeqNum = 1;


    while(true) { 
      byte[] buffer = new byte[576]; 
      DatagramPacket rpack = new DatagramPacket(buffer, buffer.length);

      
      dsock.receive(rpack);
      byte[] packet = rpack.getData();
      System.out.println(packet.length);
      decodeHeader(packet);
      System.out.println(seq_num);
      byte[] data = rpack.getData(); 
      int packSize = rpack.getLength(); 
      InetAddress remote_addr = InetAddress.getByName(remote_ip);
      DatagramPacket spack = new DatagramPacket(data, data.length, remote_addr, remote_port); 
      String s2 = new String(data, 0, packSize);   
      spack.setData("hello!".getBytes());
      System.out.println("sent data");
      dsock.send(spack); 
   } 
} 
}
