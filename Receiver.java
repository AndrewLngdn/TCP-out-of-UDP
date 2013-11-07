import java.net.*; 
import java.util.*;   

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

   public static void decodeHeader(byte[] packet){
      System.out.println("decoding header");
      byte[] header = Arrays.copyOfRange(packet, 0, 20);

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
    byte[] buffer = new byte[576]; 
    DatagramPacket dpack = new DatagramPacket(buffer, buffer.length);
    System.out.println("Started");   

    while(true) { 
     dsock.receive(dpack);
     decodeHeader(dpack);
     System.out.println("Received " + dpack.getPort() + ": " + new String(dpack.getData()));   
     byte[] data = dpack.getData(); 
     int packSize = dpack.getLength(); 
     InetAddress remote_addr = InetAddress.getByName(remote_ip);
     System.out.println(remote_addr);
     System.out.println(remote_port);
     dpack.setAddress(remote_addr);
     dpack.setPort(remote_port);
     String s2 = new String(data, 0, packSize);   
     System.out.println( new Date( ) + " " + dpack.getAddress( ) + " : " + Integer.parseInt(args[1]) + " "+ s2); 
     // byte[] data = {(byte)1};
     dpack.setData("From SERVER!!!".getBytes());
     System.out.println("sent data");
     dsock.send(dpack); 
   } 
} 
}
