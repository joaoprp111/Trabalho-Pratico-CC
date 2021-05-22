package HttpGw;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HttpGw {
	private DatagramSocket s;
	private ServerSocket ss;
	private Map<String,Connection> connections; // Chave -> porta + "-" + ip
	private Lock l;

	public HttpGw() {
		try {
			this.s = new DatagramSocket(8080);
			this.connections = new HashMap<>();
			this.l = new ReentrantLock();
			this.ss = new ServerSocket(8080);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private long byteToLong(byte[] bs){
		ByteBuffer bb = ByteBuffer.wrap(bs);
		return bb.getLong();
	}

   public void requestFileData(String filename){
		Collection<String> inactives = new ArrayList<>();
		Set<Map.Entry<String,Connection>> cs;
		try{
			l.lock();
			cs = connections.entrySet();
		} finally {
			l.unlock();
		}
			for(Map.Entry<String,Connection> par: cs){
				double currentTime = (double) System.nanoTime() / 1000000000;
				double connectionLastBeacon;
				InetAddress destIp;
				int destPort;
				Connection c = par.getValue();
				try {
					c.lock();
					connectionLastBeacon = c.getLastBeaconSeconds();
					destIp = c.getSourceIp();
					destPort = c.getSourcePort();
				} finally {
					c.unlock();
				}
				if ((currentTime - connectionLastBeacon) > 7.5) {
					//Desconectou, temos de remover
					inactives.add(par.getKey());
					System.out.println("Desconectou-se");
				} else {
					//Enviar o pedido
					FSChunkProtocol.sendMetaDataRequest(s,filename,destIp,destPort);
				}
			}

			for(Map.Entry<String,Connection> par : cs){
				String k = par.getKey();
				if(inactives.contains(k)) {
					try{
						l.lock();
						connections.remove(k);
						System.out.println("Conexao removida");
					} finally {
						l.unlock();
					}
				}
			}
	}
	private void manageServer(PDU p){
		int port = p.getPort();
		InetAddress ip = p.getIp();
		try {
			StringBuilder sb = new StringBuilder(String.valueOf(port));
			sb.append("-");
			sb.append(ip.toString());
			String key = sb.toString();

			try {
				l.lock();
				if (!connections.containsKey(key)) {
					Connection conn = new Connection(ip, port);
					connections.put(key, conn);
					System.out.println("> Gw: Servidor com a porta " + port + " e ip " + ip + " está conectado!");
				} else {
					double nextBeaconSeconds = (double) System.nanoTime() / 1000000000;
					Connection c = connections.get(key);
					try{
						c.lock();
						c.setLastBeaconSeconds(nextBeaconSeconds);
					} finally {
						c.unlock();
					}
				}
			} finally {
				l.unlock();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public void receiveFFS(){
		new Thread(() -> {
			while (true) {
				PDU p = FSChunkProtocol.receivePacket(s);

				int type = p.getType();
				switch (type) {
					case 1:
						manageServer(p);
						break;
					case 3:
						long fileSize = byteToLong(p.getData());
						System.out.println(fileSize);
						break;
					default:
						break;
				}
			}
		}).start();
	}

	public void receiveClients(){
		// Thread que aceita HTTP Requests dos clientes
		new Thread(() -> {
			try {
				while (true) {
					Socket socket = ss.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					String input;
					if((input = in.readLine()) != null) {
						String filename = (input.split(" ")[1]).split("/")[1];
						requestFileData(filename);
					}
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}).start();
	}

	public void runGateway() {
		receiveFFS();

		receiveClients();
	}

	public static void main(String[] args){
		HttpGw gw = new HttpGw();

		gw.runGateway();
	}
}
