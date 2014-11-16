import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.HashMap;



class receiver {
	
	static InetAddress addr_emulator;
	static String emulator_hostname;
	static int emulator_port; 
	static int receiver_port;
	static String name_file;

	static DatagramSocket receiverSocket;
	static DatagramSocket clientSocket;
	static int expected_packet_received = 0; 
	static boolean gotEOT = false;

	static HashMap<Integer, packet> winBuf;
	static int last_received_packet;

	static FileWriter filewriter;
	static BufferedWriter buffwriter;
	

	/*Print contents of winBuf*/
	public static void printContents(){
		for(int i = 0; i < winBuf.size(); ++i){
			System.out.println(winBuf.get(i).getData());
		}
	}
	/* Receive a packet from sender */
	public static void receive() throws Exception{
		byte[] recvBuf = new byte[1024];
	        DatagramPacket recvPacket = new DatagramPacket(recvBuf , recvBuf.length);
	        receiverSocket.receive(recvPacket);
        	packet p = packet.parseUDPdata(recvPacket.getData());
		int cur_pak_seq = p.getSeqNum();
		buffwriter.write("" + cur_pak_seq);
		buffwriter.newLine();
		if(p.getType() == 2){ //EOT packet => send EOT back to receiver
			gotEOT = true;
			System.out.println("Last one: " + cur_pak_seq);			
			packet eot = packet.createEOT(cur_pak_seq); 
			byte[] eotBuf = new byte[1024];
			eotBuf = eot.getUDPdata();
			DatagramPacket sendEOTPacket = new DatagramPacket(eotBuf, eotBuf.length, addr_emulator, emulator_port);
        	 	clientSocket.send(sendEOTPacket);
			return;
		}
		if(!winBuf.containsKey(new Integer(cur_pak_seq))){
			winBuf.put(new Integer(cur_pak_seq), p);
		}

		String str_res = new String( p.getData());
		sendACK(cur_pak_seq);

	}
	/* Send ACK back to emulator */
	public static void sendACK(int seq) throws Exception{
		byte[] sendBuf = new byte[1024];		
		packet ack = packet.createACK(seq);
        	sendBuf = ack.getUDPdata();
        	DatagramPacket sendPacket = new DatagramPacket(sendBuf , sendBuf.length , addr_emulator , emulator_port );
        	clientSocket.send(sendPacket);
	}	
	
	public static void main(String argv[]) throws Exception{
		
		if(argv.length < 4){
			System.out.println("Incorrect usage. Refer to assignment for instructions on usage");
			return;
		}

		emulator_hostname = argv[0];
		emulator_port = Integer.parseInt(argv[1]);
		receiver_port = Integer.parseInt(argv[2]);
		name_file = argv[3];		

		receiverSocket = new DatagramSocket(receiver_port);
		clientSocket = new DatagramSocket();
		addr_emulator = InetAddress.getByName(emulator_hostname);

		winBuf = new HashMap<Integer, packet>();

		filewriter = new FileWriter("arrival.log", true);
		buffwriter = new BufferedWriter(filewriter);
		while(!gotEOT){
			receive();
		}
		buffwriter.close();
		StringBuilder data = new StringBuilder();
		for(int i =0; i < winBuf.size(); ++i){
			data.append(new String(winBuf.get(i).getData()));
		}
            	String str_data = data.toString();
		FileWriter fw = new FileWriter(name_file);
		BufferedWriter fb = new BufferedWriter(fw);
		fb.write(str_data);
		fb.close();
		receiverSocket.close();
		clientSocket.close();

	}

};
