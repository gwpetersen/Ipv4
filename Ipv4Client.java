
/**
 * @author Garett Petersen
 * Due date: 2/3/2016
 * 
 * Professor: Pantic CS380
 */


import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;




public class Ipv4Client {
	public static void main(String[] args) throws IOException, InterruptedException {
		String host = "cs380.codebank.xyz";
		int port = 38003;
		short DATASIZE = 2;
		int counter = 1;
		
		byte version = 0b0100; //version 4 (4 bits)
		byte hl = 0b0101; //length of header (5) (4 bits)
		
		byte tos = 0b0; //dont implement (8 bits)
		
		short length = 0b10100; //size of header (5) + data size 
		
		byte id[] = new byte[2]; //dont implement (16 bits)
			id[0] = 0b0;// 0's of 8bits
			id[1] = 0b0;//another 0's of 8 bits
			
		byte flags = 0b010; //dont fragment  (3 bits)
		byte fragOffset[] = new byte[2]; //13 bits, don't implement
			fragOffset[0] = 0b0;
			fragOffset[1] = 0b0;
			
		byte ttl = 0b00110010; //time to live 50 (1 byte)
		
		byte protocol = 0b0110; //TCP protocol (6) [1 byte]
		
		byte source[] = new byte[4]; // source ip doesnt matter could be anything
			source[0] = 0b0; //0
			source[1] = 0b0;		//0
			source[2] = 0b00; //0
			source[3] = 0b00; //0
			
		byte[] destip;
		
			
		
		try (Socket socket = new Socket(host, port)) { 
			BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintStream out = new PrintStream(socket.getOutputStream());
			System.out.println("Connected to " + host + ":" + port + "!\n");
			InetAddress ServerIP = socket.getInetAddress();
			//destip contains the server ip(destinationip)
			destip = ServerIP.getAddress();
			
			//this will start a thread that listens to the server
			//what the server sends will be read in the listener class.
			
			listener listen = new listener(socket);
			Thread thread =new Thread(listen);
			thread.start();// reads the incoming messages from server
			
			
			// just used to see the server ip...was getting 
			// an error just used for testing reasons
			for (int i =0; i<destip.length;i++){
			//System.out.print(destip[i]+ " ");

			}
			
			Scanner kb = new Scanner(System.in);
			/**
			 * this while loop will be where I'm assigning
			 * each field to its correct position in my packet
			 * I'm sending a total of 12 packets to the server.
			 * Datasize is doubling each time therefore so will the size
			 * of the packet.
			 * 
			 */
			while(counter < 13) { // sending 12 total packets
				kb.nextLine();
				byte[] packet = new byte[20 + DATASIZE]; //first one is 20+2,second is 20+4...
				byte data[] = new byte[DATASIZE];//size is initially set to 2
				
				// whats in data doesnt matter only size of data does
				
				packet[0] = version;
				packet[0] <<= 4;
				/**
				 * example:
				 * if
				 * a = 0011 1100 b = 0000 1101
				 * then
				 * a|b = 0011 1101
				 */

						
				packet[0] |= hl;
				
				packet[1] = tos;
			
				// total length = head length +  data size
				short tempshort = (short) (length + DATASIZE); 
				//dont want signed bits and also the number of bits in total
				// length field can only be 16
				byte ttl2 = (byte) ((tempshort >>> 8) & 0xFF);
				byte ttl1 = (byte) (tempshort & 0xFF); //holds the unsigned total length

				packet[2] = ttl2;
				packet[3] = ttl1; //will change based on doubling data size
				
				packet[4] = id[1];
				packet[5] = id[0];
				
				packet[6] = flags;
				packet[6] <<= 5;
				
				packet[6] |= fragOffset[0];
				packet[7] = fragOffset[1];
				
				packet[8] = ttl;
				
				packet[9] = protocol;
				
				// skipping the checksum for later..
				
				for(int i=12, j=0; i<16; ++i, ++j) 
					packet[i] = source[j];
				
				for(int i=16, j=0; i<20; ++i, ++j) 
					packet[i] = destip[j];
				
				for(int i=20, j=0; j<data.length; ++i, ++j)
					packet[i] = data[j];
				
				// not dealing with signed numbers
				// shortarray now has all fields other than the checksum
				// use this short array to find checksum
				short[] shortarray = byteToShort(packet);
				
				short[] returnedcheck= new short[1]; // the array containing the checksum
				/**
				   * 
				   * 1) apply a 16-bit 1's complement sum over all fields (adjacent 8-bit pairs [A,B], final odd length is [A,0])
				   * 2) apply 1's complement to this final sum
				   *
				   * 
				   * 1's complement is bitwise NOT of positive value.
				   * Ensure that any carry bits are added back to avoid off-by-one errors..will be noted below
				   * 
				   * next steps calculate checksum of all fields in packet excluding the checksum itself
				   * after checksum is found its put into the array 'returnedcheck'
				   *
				   */
				
				returnedcheck[0] = (short) checkSum(shortarray);
				
				// taking the checksum returned and passing it to the byte array translation
				byte checksum[] = toByteArray(returnedcheck);
				
				// now all thats left is to put the checksum into its correct location in the
				// packet.

				packet[10] = checksum[0]; 
				packet[11] = checksum[1];
				
				// at this point the packet is complete...send the packet to the server
				// move the counter up by 1...to keep track that we have sent 12 total packets
				// multiply the data size by 2 as specified in directions..2bytes,4bytes,6bytes..ect
				
				out.write(packet);
				System.out.println("Packet sent: " +counter);
				DATASIZE *= 2; // original data size(2bytes) is doubled each time 
				counter ++; // increment every time a packet is sent
			}
			System.out.println("Done");
			socketIn.close();
			out.close();
		}
		System.exit(0);
	}
	/**
	 * getting the adjacent 8 bit pairs
	 * @param packet
	 * @return
	 */
	private static byte[] toByteArray(short[] packet) {
		int j = 0;
		byte[] bArray = new byte[(packet.length << 1)];
		for (int i = 0; i < packet.length; ++i) {
	    	bArray[j + 1] |= (packet[i] & 0xFF);
	    	packet[i] >>>= 8;
	    	bArray[j] |= (packet[i] & 0xFF);
	    	j += 2;
		}
		return bArray;
    }
	/**
	   * 
	   * 1) apply a 16-bit 1's complement sum over all fields (adjacent 8-bit pairs [A,B], 
	   *    final odd length is [A,0])
	   * 2) apply 1's complement to this final sum
	   *
	   * 
	   * 1's complement is bitwise NOT of positive value. so you need it to be unsigned
	   * carry bits must be added back as shown below...
	   *
	   *
	   */
	private static long checkSum(short[] buf) { 
		long sum = 0;
		for(int i=0; i<10; ++i) { 
			sum += (buf[i] & 0xFFFF);
			
			// 1's complement carry bit correction in 16-bits (detecting sign extension)
			if ((sum & 0xFFFF0000) > 0) { 
				sum &= 0xFFFF;
				sum++;
			}
		}
		sum = ~(sum & 0xFFFF);
		return sum;
	}

	private static short[] byteToShort(byte[] message) {
		short[] shortMessage = new short[(message.length + 1) / 2];
		for (int i = 0, j = 0; j < message.length - 1; i++, j += 2) {
			shortMessage[i] |= (message[j] & 0xFF);
			shortMessage[i] <<= 8;
			shortMessage[i] |= (message[j + 1] & 0xFF);
		}
		return shortMessage;
	}
	

}