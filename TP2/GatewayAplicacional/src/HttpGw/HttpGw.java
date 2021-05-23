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
import java.util.stream.Collectors;

public class HttpGw {
	private DatagramSocket s;
	private ServerSocket ss;
	private Map<String,Connection> connections; // Chave -> porta + "-" + ip
	private int numTransfers;
	private Lock l;

	public HttpGw() {
		try {
			this.s = new DatagramSocket(8080);
			this.connections = new HashMap<>();
			this.l = new ReentrantLock();
			this.ss = new ServerSocket(8080);
			this.numTransfers = 0;
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

	public void waitForReplies(String filename){
		int numServers, n = 0;
		try {
			l.lock();
			numServers = connections.keySet().size();
		} finally {
			l.unlock();
		}

		int serversToAsk = (int) (0.8 * numServers);
		System.out.println("Num servidores utilizados na transferência: " + serversToAsk);

		while (n < serversToAsk) {
			PDU p = FSChunkProtocol.receivePacket(s);

			int type = p.getType();
			if (type == 3) {
				long fileSize = byteToLong(p.getData());
				String key = new StringBuilder(String.valueOf(p.getPort())).append("-").append(p.getIp()).toString();
				try{
					l.lock();
					Connection c = connections.get(key);
					try{
						c.lock();
						c.setCurrentFileTransfer(filename);
						c.setCurrentFileSize(fileSize);
					} finally {
						c.unlock();
					}
					connections.replace(key,c);
				} finally {
					l.unlock();
				}
			}
			n++;
		}
		System.out.println("Respostas recebidas, pronto para começar a transferência!");
	}

	public void beginTransfer(String file){
		Collection<Connection> cs;
		long offset = 0;
		try{
			l.lock();
			cs = connections.values().stream().filter(c -> (c.getCurrentFileTransfer().equals(file))).collect(Collectors.toCollection(ArrayList::new));
		} finally {
			l.unlock();
		}
		Iterator it = cs.iterator();
		long fileSize;
		if(it.hasNext()) {
			System.out.println("it.hasnext com sucesso!");
			Connection c = (Connection) it.next();
			try{
				c.lock();
				fileSize = c.getCurrentFileSize();
			} finally {
				c.unlock();
			}
			System.out.println("Cs.size : " + cs.size());
			long fragments = fileSize / cs.size();
			long actualChunkSize = (fragments > 512) ? 512 : fragments;
			System.out.println("Tamanho dos chunks a pedir: " + actualChunkSize);

			while(offset <= fileSize) {
				System.out.println("Offset : " + offset);
				it = cs.iterator();
				// Falta verificar se uma destas conexões já não existe

				while (it.hasNext()) {
					c = (Connection) it.next();
					FSChunkProtocol.sendTransferRequest(s, file, offset, actualChunkSize, c.getSourceIp(), c.getSourcePort(), numTransfers);
					offset += 1;
					offset *= actualChunkSize;
				}
			}
		}
	}

	public void receiveTransfer(String filename){

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
						waitForReplies(filename);
						beginTransfer(filename);
						numTransfers++;
						receiveTransfer(filename);
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
