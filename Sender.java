import java.net.*; 
import java.util.*;  
import java.nio.*;
import java.io.*;
import java.security.*;


public class Sender implements Runnable { 

  // ints to four bytes
  public static byte[] convertToFourBytes(int value) {
      byte[] r = new byte[4];
      r[0] = (byte)(value >>> 24);
      r[1] = (byte)(value >>> 16);
      r[2] = (byte)(value >>> 8);
      r[3] = (byte)value;
      return r;
  }

  //ints to two bytes
  public static byte[] convertToTwoBytes(int i){
    byte[] result = new byte[2];
    result[0] = (byte) (i >> 8);
    result[1] = (byte) (i /*>> 0*/);
    return result;
  }

  // used to put together byte arrays
  public static byte[] concat(byte[] first, byte[] second) {
    byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  // get an int from a byte array
  static Integer intFromByteArray(byte[] bytes) {
    if (bytes.length != 4){
      bytes = concat(new byte[2], bytes);
    }
    return (Integer)ByteBuffer.wrap(bytes).getInt();
  }

  // used to decode the ack header
  public static Hashtable<String, Object> decodeHeader(byte[] packet){
    Hashtable<String, Object> headerHash = new Hashtable<String, Object>(); 

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

    headerHash.put("seq_num", intFromByteArray(seq_num_a));
    headerHash.put("ack_num", intFromByteArray(ack_num_a));
    headerHash.put("source_port", intFromByteArray(source_port_a));
    headerHash.put("dest_port", intFromByteArray(dest_port_a));
    headerHash.put("fin_flag", (Boolean)(flags[0] == (byte)1));
    return headerHash;
  }


  //used to make the packet header
  public static byte[] makeHeader(int sPort, int dPort, int seq_num, boolean fin, byte[] checksum){
    byte[] header = new byte[20];

    byte[] sourcePort = convertToTwoBytes(sPort);
    byte[] destinationPort = convertToTwoBytes(dPort);
    byte[] sequenceNumber = convertToFourBytes(seq_num);
    byte[] ack = convertToFourBytes(123456);
    byte[] flags; 

    if (fin){
      flags = convertToTwoBytes(1);
    } else {
      flags = convertToTwoBytes(0);
    }

    byte[] recWindow = convertToTwoBytes(1);
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


  // opens a file for the logger 
  public static BufferedWriter openLogFile(String filename){
    if (filename.equals("stdout")){
      return new BufferedWriter(new OutputStreamWriter(System.out));
    }

    try {
      File file = new File(filename);

      if (!file.exists()) {
        file.createNewFile();
      }
 
      FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
      return new BufferedWriter(fw);

    } catch (IOException e) {
      System.out.println("Couldn't create logfile");
      e.printStackTrace();
    }
    return null;
  }

  // all the stuff about the packet
  public static ArrayList<byte[]> packets = new ArrayList<byte[]>(); // holds packets for resending
  public static int nextseqnum = 0;     // sequence numbers start at 0
  public static int base = 0;           // base of window
  public static int timeout = 50;       // timout
  public static String filename;        // file to send out
  public static String remote_ip;       // remote host
  public static int remote_port;        // port to send data to
  public static int ack_port;           // port to recieve udp acks
  public static int window_size;        // 
  public static int received_ack = 0;   // latest received ack
  public static byte[] file_bytes;      // byte array of file
  public static DatagramSocket dsock;   // socket to send over
  public static InetAddress remote_addr;
  public static int number_of_packets_needed = -1;  // number of packets needed to send entire file
  public static int payload_size = 576; // size of entire packet
  public static BufferedWriter bw;

  public static void main( String args[] ) throws Exception { 

    if (args.length != 6){
      System.out.println("java Sender [filename] [remote_IP] [remote_port] [ack_port_number] [window_size] [log_filename]");
      System.exit(1);
    } 

    filename = args[0];
    remote_ip = args[1];
    remote_port = Integer.parseInt(args[2]);
    ack_port = Integer.parseInt(args[3]);
    window_size = Integer.parseInt(args[4]);
    String log_filename = args[5];
    remote_addr = InetAddress.getByName(remote_ip); 

    bw = openLogFile(log_filename);

    // Load file as bytes
    FileInputStream fileInputStream = null;
    File file = new File(filename);
    file_bytes = new byte[(int)file.length()];

    try {

      fileInputStream = new FileInputStream(file);
      fileInputStream.read(file_bytes);
      fileInputStream.close();

      number_of_packets_needed = file_bytes.length/(payload_size-20) + 1;

    } catch (IOException e) {
      e.printStackTrace();
    }

    // this block starts the ack listener thread 
    // and also spawns off a thread for sending packets 
    try {

      dsock = new DatagramSocket(ack_port);
      new Thread(new Sender()).start();

      // listen for acks
      while (true){
        byte[] buffer = new byte[576];

        DatagramPacket dpack = new DatagramPacket(buffer, buffer.length);

        dsock.receive(dpack);          // receive the ack.
        Hashtable<String, Object> header = decodeHeader(dpack.getData()); // decode the header and load the ack number
        Integer received_ack = (Integer) header.get("ack_num");
        base = received_ack + 1;       // into recieved_ack

        Integer seq_num = (Integer) header.get("seq_num");
        Integer source_port = (Integer) header.get("source_port");
        Integer dest_port = (Integer) header.get("dest_port");
        Integer ack_num = (Integer) header.get("ack_num");
        Boolean local_fin = (Boolean)header.get("fin_flag");
        String log_entry = "Time(ms): " + System.currentTimeMillis() + " ";
        log_entry += "Source:" + remote_ip + ":" + source_port + " ";
        log_entry += "Destination:" + InetAddress.getLocalHost() + ":" + dest_port + " ";
        log_entry += "Ack_num:" + ack_num + " ";
        log_entry += "fin? " + local_fin + "";

        oldRTT = System.currentTimeMillis()-packetTimer.get(seq_num);
        log_entry += "RTT " + oldRTT + "\n";


        bw.write(log_entry);
        bw.flush();

        timeout -= Math.min(50, timeout - 1);

        // if we get all the acks, then we're done!
        if (received_ack == number_of_packets_needed - 1){
          System.out.println("+----------------------------------<");
          System.out.println("|Delivery completed successfully!!!");
          System.out.println("|Total bytes sent = " + file_bytes.length);
          System.out.println("|Total segments sent = " + total_packets_sent);
          System.out.println("|Segments retransmitted = " + resent_packet_count);
          System.out.println("+----------------------------------<");

          bw.close();
          System.exit(0);
        }
      }

    } catch (Exception e){
      e.printStackTrace();
    }
  } 

  // packet maker, checks the sequence number and calculates what section of the 
  // data should be packaged and sent
  public static byte[] make_pkt(int nextseqnum, byte[] file_bytes, int ack_port, int remote_port){
    int data_start = nextseqnum * 556;
    int data_end = Math.min((nextseqnum + 1)*556, file_bytes.length);

    byte[] packet_data = Arrays.copyOfRange(file_bytes, data_start, data_end);
    byte[] packet = null;

    try {
      MessageDigest md = MessageDigest.getInstance("MD5");

      if (packet_data.length != 556){
        byte[] temp = new byte[556];
        System.arraycopy(packet_data, 0, temp, 0, packet_data.length);
        md.update(temp);
      } else {
        md.update(packet_data);
      }

      byte[] md5bytes = md.digest();
      byte[] checksum = new byte[2];
      checksum[0] = md5bytes[0];
      checksum[1] = md5bytes[1];

      // calculate checksum
      byte[] header;

      if (nextseqnum == number_of_packets_needed-1){
        // set the fin flag if we're on the last packet
        header = makeHeader(ack_port, remote_port, nextseqnum, true, checksum); // get things from string args
      } else {
        header = makeHeader(ack_port, remote_port, nextseqnum, false, checksum); // get things from string args
      }

      packet = concat(header, packet_data);   // packet = header + data
      packets.add(packet);
    } catch (Exception e){
      e.printStackTrace();
    }

    return packet;
  }

  public void resendPackets(){
    try {
      for (int i = base; i < packets.size(); i++){
        resent_packet_count++;

        byte[] packet = packets.get(i);
        DatagramPacket dpack = new DatagramPacket(packet, packet.length, remote_addr, remote_port); 

        total_packets_sent++;
        Hashtable<String, Object> header = decodeHeader(dpack.getData()); // decode the header and load the ack number
        Integer seq_num = (Integer) header.get("seq_num");
        Integer source_port = (Integer) header.get("source_port");
        Integer dest_port = (Integer) header.get("dest_port");
        Integer ack_num = (Integer) header.get("ack_num");
        Boolean local_fin = (Boolean)header.get("fin_flag");
        String log_entry = "Time(ms): " + System.currentTimeMillis() + " ";
        log_entry += "Source:" + InetAddress.getLocalHost() + ":" + dest_port + " ";
        log_entry += "Destination:" + remote_ip + ":" + source_port + " ";
        log_entry += "Ack_num:" + "n/a" + " ";
        log_entry += "fin? " + local_fin + "\n";

        bw.write(log_entry);

        packetTimer.add(i, (Long)System.currentTimeMillis());
        dsock.send(dpack);
      }
    } catch (IOException e){
      e.printStackTrace();
    }
  }
  public static Long oldRTT; 
  public static long timeoutTimer;
  public static ArrayList<Long> packetTimer = new ArrayList<Long>();
  public static int total_packets_sent = 0;
  public static int resent_packet_count = 0;
  public void run(){
    long calculatedTimeout = 1;
    try {
      while(true){

        // check for timeout
        long elapsedTime = System.currentTimeMillis() - timeoutTimer;
        System.out.println("timer: " + timeout);
        if (elapsedTime > timeout){
          timeoutTimer = System.currentTimeMillis();
          timeout *= 2;
          // timeoutTimer = System.currentTimeMillis();
          resendPackets();
        }

        if (nextseqnum < base+window_size && nextseqnum < number_of_packets_needed){
          // make packet here
          byte[] packet = make_pkt(nextseqnum, file_bytes, ack_port, remote_port);
          DatagramPacket dpack = new DatagramPacket(packet, packet.length, remote_addr, remote_port); 

          total_packets_sent++;
          Hashtable<String, Object> header = decodeHeader(dpack.getData()); // decode the header and load the ack number
          Integer seq_num = (Integer) header.get("seq_num");
          Integer source_port = (Integer) header.get("source_port");
          Integer dest_port = (Integer) header.get("dest_port");
          Integer ack_num = (Integer) header.get("ack_num");
          Boolean local_fin = (Boolean)header.get("fin_flag");
          String log_entry = "Time(ms): " + System.currentTimeMillis() + " ";
          log_entry += "Source:" + InetAddress.getLocalHost() + ":" + dest_port + " ";
          log_entry += "Destination:" + remote_ip + ":" + source_port + " ";
          log_entry += "Ack_num:" + "n/a" + " ";
          log_entry += "fin? " + local_fin + "\n";


          bw.write(log_entry);
          bw.flush();
          dsock.send(dpack); // send the packet 
          packetTimer.add(nextseqnum, (Long)System.currentTimeMillis());
          nextseqnum++;
          
          if (base == nextseqnum){
            timeoutTimer = System.currentTimeMillis();
          }
        }
      }
    } catch (Exception e){
      System.err.println("Eception: ");
      e.printStackTrace();
    }
  }
} 
