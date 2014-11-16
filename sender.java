import java.io.*;
import java.net.*;
import java.util.Vector;


/*Run the recvAck function as a different thread*/
class Server implements Runnable{
        public void run(){
            try{
                while(!sender.isFinish || !sender.gotEOT) {
                    sender.receiveACK();
                }
            }catch (Exception e){ return; }

        }
};


class sWindow{
        public int timeout;
        public int timeExpired;
        public boolean ack;
        public int seqnum;
        public packet pack;

        public sWindow(int timet, packet packt){
                timeout = timet;
                timeExpired = 0;
                seqnum = packt.getSeqNum();
                pack = packt;
                ack = false;
        }

};




class sender {

	static DatagramSocket senderSocket;
	static DatagramSocket serverSocket;
	static Vector<sWindow> sWinSend;	

	static String emulator_addr;
	static int emulator_port;
	static int sender_port;
	static String name_file;
	
	static int packet_size = 500;
	static int timeout = 1500;
        static InetAddress addr_emulator;
	static int cur_packet = 0;
	static int windowSize = 10;
	static int startIndex = 0;
	static boolean gotEOT = false;
	static boolean isFinish = false;

	//Log file variables
	static FileWriter logWr;
	static BufferedWriter logBuf;

	public static void printWindow() throws Exception{
		for(int i = 0; i < sWinSend.size(); ++i){
			System.out.println(i);		
		}
	}

	/*Initialize data by storing it into sWindow. Send only the first 10 packets first.*/
	public static void initData(String strData) throws Exception{
		int lenData = strData.length();
		int num_packets = lenData / packet_size;
		
		for(int i = 0; i < num_packets; ++i){
			String str = strData.substring(i*packet_size, (i+1)*packet_size);
			packet p = packet.createPacket(cur_packet++, str);			
			sWinSend.add(new sWindow(timeout, p)); 
		}
		
		int starting_point = num_packets*packet_size;
		String str = strData.substring(starting_point, lenData);
		packet p = packet.createPacket(cur_packet++, str);			
		sWinSend.add(new sWindow(timeout, p));
		
		for(int i = 0; i < windowSize; ++i){
			if(i < sWinSend.size())	send(sWinSend.get(i).pack);
		}
		monitor(); //wait for all ACKS
	}

	/*send a packet to emulator*/
	public static void send(packet p) throws Exception{		
		byte[] p_arr = p.getUDPdata();
		DatagramPacket sendPacket = new DatagramPacket(p_arr, p_arr.length , addr_emulator , emulator_port);
		addLog(p);                
		senderSocket.send(sendPacket);
	}

	/*after receiving ack, update window, and send next packets according to SR protocol.*/
	public static void updateWindow(packet p) throws Exception{
		int type = p.getType();
		if(type == 2){ //EOT
			gotEOT = true;	
			return;	
		}
		int cur_seq = p.getSeqNum();
		int index = -1;
		
		for(int i = startIndex; i < (startIndex + windowSize) && i < sWinSend.size(); ++i){
			if(sWinSend.get(i).pack.getSeqNum() == cur_seq){index = i; break;}
		}
		if(index == startIndex){ 
			while(startIndex < sWinSend.size() && sWinSend.get(startIndex).ack){
				if(startIndex + windowSize < sWinSend.size()){
				send(sWinSend.get(startIndex + windowSize).pack);
				++startIndex;
				} else{ break;}
			}
			if(startIndex == sWinSend.size()) isFinish = true;
		} else if(index < sWinSend.size() && index > startIndex){
			sWinSend.get(index).ack = true;
		} 

				
		boolean all_acks = true;
		for(int i = 0; i < sWinSend.size(); ++i){
			if(!sWinSend.get(i).ack) all_acks = false;
			
		}
		isFinish = all_acks;
		
	}

	/* Receive ACKS from emulator and set ACK field of incoming packet to be true to make it marked as received. */
	public static void receiveACK() throws Exception{
		byte[] recvBuf = new byte[1024];
		DatagramPacket recvPacket = new DatagramPacket(recvBuf , recvBuf.length);
		serverSocket.receive(recvPacket);
		packet p = packet.parseUDPdata(recvPacket.getData());
		System.out.println("Received ACK " + p.getSeqNum()); 
		sWinSend.get(p.getSeqNum()).ack = true;
		addLog(p);
		updateWindow(p);
	}
	
	/*Check if any packets need to be resent, if the packet's timer expired. If all packets are ACKD, then send EOT*/
	public static void monitor() throws Exception{
		Thread.sleep(50);
		while(!isFinish){
			for(int i = startIndex; i < sWinSend.size(); ++i){
				sWindow swCur = sWinSend.get(i);
				if(!swCur.ack){
					swCur.timeExpired += 1;
					if(swCur.timeExpired >= timeout) {
						send(swCur.pack);
						swCur.timeExpired = 0;
					}
				}
			
			}
		}
		packet eot = packet.createEOT(sWinSend.size()); 
        	send(eot);
	}
	
	/*Record logs of sequence numbers and acks into seqnum.log or ack.log, depending on the type of packet.*/
	public static void addLog(packet p) throws Exception{
		String logFile;
		if(p.getType() == 1) logFile = "seqnum.log";
		else if(p.getType() == 0) logFile = "ack.log";		
		else return;
		logWr = new FileWriter(logFile, true);
		logBuf = new BufferedWriter(logWr);
		int p_seqNum = p.getSeqNum();	
		if(p.getType() == 1) System.out.println("Sending packet number: " + p_seqNum);
				
		logBuf.write("" + String.valueOf(p_seqNum));
		logBuf.newLine();
		logBuf.close();
	}

	public static void main(String argv[]) throws Exception{

		if(argv.length < 4){
			System.out.println("Incorrect usage. Refer to assignment for instructions on usage");
			return;
		}
		emulator_addr = argv[0];
		emulator_port = Integer.parseInt(argv[1]);
		sender_port = Integer.parseInt(argv[2]);
		name_file = argv[3];

		addr_emulator = InetAddress.getByName(emulator_addr);
		senderSocket = new DatagramSocket();
		serverSocket = new DatagramSocket(sender_port);

		sWinSend = new Vector<sWindow>();
		Server server = new Server();
		Thread t = new Thread(server, "server");
		t.start();
		
		//Log files
		logWr = new FileWriter("seqnum.log");
		logBuf = new BufferedWriter(logWr);

		FileReader in = new FileReader(name_file);
		StringBuilder data = new StringBuilder();
		char[] buffer = new char[4096];
		int read = 0;
		while(read >= 0){
            		data.append(buffer, 0, read);
            		read = in.read(buffer);
        	} 
            	String str_data = data.toString();
		initData(str_data);
		senderSocket.close();
		serverSocket.close();	
	}


};




