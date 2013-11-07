import java.net.*; 
import java.util.*;   
public class ServerEcho { 
 public static void main( String args[]) throws Exception { 
  DatagramSocket dsock = new DatagramSocket(Integer.parseInt(args[0])); 
  System.out.println("Starting server " + dsock.getPort());   
  byte arr1[] = new byte[150]; 
  DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );
  System.out.println("Started");   
  while(true) { 
   dsock.receive(dpack);
   System.out.println("Received " + dpack.getPort());   
   byte arr2[] = dpack.getData(); 
   int packSize = dpack.getLength(); 
   dpack.setPort(Integer.parseInt(args[1]));
   String s2 = new String(arr2, 0, packSize);   
   System.out.println( new Date( ) + " " + dpack.getAddress( ) + " : " + Integer.parseInt(args[1]) + " "+ s2); 
   dsock.send(dpack); 
  } 
 } 
}
