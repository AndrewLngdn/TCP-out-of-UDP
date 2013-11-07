import java.net.*; 
import java.util.*;   
public class EchoClient { 
 public static void main( String args[] ) throws Exception { 
  InetAddress add = InetAddress.getByName("localhost");   
  System.out.println("Starting the client "+args[0] + "," + args[1]); 
  DatagramSocket dsock = new DatagramSocket(Integer.parseInt(args[1])); 
  System.out.println("Client socket = " + dsock.getPort()); 
  String message1 = "This is client calling"; 
  byte arr[] = message1.getBytes( ); 
  DatagramPacket dpack = new DatagramPacket(arr, arr.length, add, Integer.parseInt(args[0])); 
  while(true){
   System.out.println("Sending the packet "); 
   dpack = new DatagramPacket(arr, arr.length, add, Integer.parseInt(args[0]));
   System.out.println("Sending the packet to " + dpack.getPort());
   dsock.send(dpack); // send the packet 
   Date sendTime = new Date( ); // note the time of sending the message   
   //dsock.receive(dpack); // receive the packet 
   String message2 = new String(dpack.getData( )); 
   Date receiveTime = new Date( ); // note the time of receiving the message 
   System.out.println((receiveTime.getTime( ) - sendTime.getTime( )) + " milliseconds echo time for " + message2); 
   Thread.sleep(1000);
  }  
 } 
} 
