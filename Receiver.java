import java.net.*; 
import java.util.*;   
import java.nio.*;
import java.util.Random;
import java.security.MessageDigest;
import java.io.*;
import java.security.*;

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

  static Integer intFromByteArray(byte[] bytes) {
    if (bytes.length != 4){
      bytes = concat(new byte[2], bytes);
    }
    return (Integer)ByteBuffer.wrap(bytes).getInt();
  }

  public static byte[] concat(byte[] first, byte[] second) {
    byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  public static boolean checksumIsRight(byte[] packet){
    byte[] checksum = Arrays.copyOfRange(packet, 16, 18);
    byte[] packet_data = Arrays.copyOfRange(packet, 20, packet.length);

    boolean match = false;
    try {
      
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(packet_data);

      byte[] md5bytes = md.digest();
      byte[] new_checksum = new byte[2];
      new_checksum[0] = md5bytes[0];
      new_checksum[1] = md5bytes[1];

      if (new_checksum[0] == checksum[0] && new_checksum[1] == checksum[1]){
        match = true;
      }

    } catch (Exception e){

    }
    return match;
  }

  public static Hashtable<String, Object> decodeHeader(byte[] packet){
    Hashtable<String, Object> headerHash = new Hashtable<String, Object>(); 

    byte[] header = Arrays.copyOfRange(packet, 0, 20);
    byte[] packet_data = Arrays.copyOfRange(packet, 20, packet.length);

    byte[] source_port_a = Arrays.copyOfRange(header, 0, 2);
    byte[] dest_port_a = Arrays.copyOfRange(header, 2, 4);
    byte[] seq_num_a = Arrays.copyOfRange(header, 4, 8);
    byte[] ack_num_a = Arrays.copyOfRange(header, 8, 12);
    byte[] header_len = Arrays.copyOfRange(header, 12, 13); // always 20
    byte[] flags = Arrays.copyOfRange(header, 13, 14);
    byte[] rec_window = Arrays.copyOfRange(header, 14, 16);
    byte[] checksum = Arrays.copyOfRange(header, 16, 18);
    byte[] urg = Arrays.copyOfRange(header, 18, 20);

    headerHash.put("seq_num", intFromByteArray(seq_num_a));
    headerHash.put("ack_num", intFromByteArray(ack_num_a));
    headerHash.put("source_port", intFromByteArray(source_port_a));
    headerHash.put("dest_port", intFromByteArray(dest_port_a));
    headerHash.put("fin_flag", (Boolean)(flags[0] == (byte)1));
    return headerHash;

  }

  public static BufferedWriter openLogFile(String filename){

    if (filename.equals("stdout")){
      return new BufferedWriter(new OutputStreamWriter(System.out));
    }

    try {
      File file = new File(filename);

      if (!file.exists()) {
        file.createNewFile();
      }
 
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      return new BufferedWriter(fw);

    } catch (IOException e) {
      System.out.println("Couldn't create logfile");
      e.printStackTrace();
    }
    return null;
  }

  public static boolean fin = false;
  public static void main( String args[]) throws Exception { 

    if (args.length != 5){
      System.out.println("java Receiver [filename] [listening_port] [remote_ip] [remote_port] [log_filename]");
      System.exit(1);
    }

    byte[] file_bytes = null; 

    String filename = args[0];
    int listening_port = Integer.parseInt(args[1]);
    String remote_ip = args[2];
    int remote_port= Integer.parseInt(args[3]);
    String log_filename = args[4];

    BufferedWriter bw = openLogFile(log_filename);

    DatagramSocket dsock = new DatagramSocket(listening_port); 

    int expected_seq_num = 0;
    int count = 0;

    while(!fin) { 

      byte[] buffer = new byte[576]; 
      DatagramPacket rpack = new DatagramPacket(buffer, buffer.length);

      dsock.receive(rpack); 


      byte[] packet = rpack.getData();
      Hashtable<String, Object> header = decodeHeader(packet);
      Integer seq_num = (Integer) header.get("seq_num");
      Integer source_port = (Integer) header.get("source_port");
      Integer dest_port = (Integer) header.get("dest_port");
      Integer ack_num = (Integer) header.get("ack_num");
      Boolean local_fin = (Boolean)header.get("fin_flag");


      String log_entry = "Time(ms): " + System.currentTimeMillis() + " ";
      log_entry += "Source:" + remote_ip + ":" + source_port + " ";
      log_entry += "Destination:" + InetAddress.getLocalHost() + ":" + dest_port + " ";
      log_entry += "Ack_num:" + "n/a" + " ";
      log_entry += "fin? " + local_fin + "\n";

      bw.write(log_entry);
      bw.flush();

      Random r = new Random();
      int rand = r.nextInt();

      if (seq_num == expected_seq_num && checksumIsRight(packet) && rand%4!=0){ // also do checksum

        fin = local_fin;

        byte[] data_without_header = Arrays.copyOfRange(packet, 20, packet.length);

        if (file_bytes == null){
          file_bytes = data_without_header;
        } else {
          file_bytes = concat(file_bytes, data_without_header);
        }

        //send ack
        byte[] ack_pack_data = makeHeader(listening_port, remote_port, expected_seq_num); 
        expected_seq_num++;
        InetAddress remote_addr = InetAddress.getByName(remote_ip);
        DatagramPacket spack = new DatagramPacket(ack_pack_data, ack_pack_data.length, remote_addr, remote_port); 

        log_entry = "Time(ms): " + System.currentTimeMillis() + " ";
        log_entry += "Source:" + InetAddress.getLocalHost() + ":" + dest_port + " ";
        log_entry += "Destination:" + remote_ip + ":" + source_port + " ";
        log_entry += "Ack_num:" + expected_seq_num + " ";
        log_entry += "fin? " + local_fin + "\n";

        bw.write(log_entry);
        bw.flush();

        dsock.send(spack); 

    } 

    bw.close();
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(file_bytes);
      fos.close();
      System.out.println("+-------------------------------------------------------<");
      System.out.println("|Delivery completed successfully! New file: " + filename);
      System.out.println("+-------------------------------------------------------<");
    } catch (Exception e){
      System.out.println("Unable to create output file");
    }
    
  } 
}
