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
	private Map<String,Long> files;
	private int numTransfers;
	private Lock l;

	public HttpGw() {
		try {
			this.s = new DatagramSocket(8080);
			this.connections = new HashMap<>();
			this.files = new HashMap<>();
			this.l = new ReentrantLock();
			this.ss = new ServerSocket(8080);
			this.numTransfers = 0;
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private byte[] removeTrash(byte[] source){
		byte[] dest;
		int i = 0;
		while(source[i] != 0){
			i++;
		}
		dest = new byte[i];
		System.arraycopy(source,0,dest,0,i);
		return dest;
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
				if ((currentTime - connectionLastBeacon) > 30) {
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

	/*public void waitForReplies(String filename){
		int numServers, n = 0;
		try {
			l.lock();
			numServers = connections.keySet().size();
		} finally {
			l.unlock();
		}

		int serversToAsk = (int) (0.8 * numServers);
		System.out.println("Num servidores utilizados na transferência: " + serversToAsk);

		// Vamos esperar n respostas em relação ao ficheiro que queremos
		while (n < serversToAsk) {
			PDU p = FSChunkProtocol.receivePacket(s);

			int type = p.getType();
			if (type == 3) {
				System.out.println("3");
				long fileSize = byteToLong(p.getData());
				String key = new StringBuilder(String.valueOf(p.getPort())).append("-").append(p.getIp()).toString();
				try{
					l.lock();
					Connection c = connections.get(key);
					try{
						c.lock();
						c.setCurrentFileTransfer(filename);
						c.setCurrentFileSize(fileSize);
						System.out.println("Ficheiro atualizado na conexão");
					} finally {
						c.unlock();
					}
					connections.replace(key,c);
				} finally {
					l.unlock();
				}
				n++;
			}
			else if(type == 1){
				System.out.println("1");
			}
		}
		System.out.println("Respostas recebidas, pronto para começar a transferência!");
	}

	public boolean beginTransfer(String file){
		Collection<Connection> cs;
		long offset = 0, fileSize = 1000000000; // Número grande para entrar pelo menos 1x no ciclo
		try{
			l.lock();
			cs = connections.values().stream()
					.filter(c -> (c.getCurrentFileTransfer().equals(file)))
					.collect(Collectors.toCollection(ArrayList::new));
		} finally {
			l.unlock();
		}
		System.out.println("Cs.size : " + cs.size());
		long fragments = 0, actualChunkSize = 0;
		int numMessages = 0;
		if(cs.size() == 0)
			return false;

		while(offset < fileSize){
			for(Connection c : cs) {
				if(offset == 0) {
					try {
						c.lock();
						fileSize = c.getCurrentFileSize();
						System.out.println("File Size : " +  fileSize);
					} finally {
						c.unlock();
					}
					fragments = fileSize / cs.size();
					actualChunkSize = (fragments > 512) ? 512 : fragments;
					System.out.println("Tamanho dos chunks a pedir: " + actualChunkSize);
				}

				System.out.println("Offset : " + offset);

				// Falta verificar se uma destas conexões já não existe

				try{
					l.lock();
					FSChunkProtocol.sendTransferRequest(s, file, offset, actualChunkSize, c.getSourceIp(), c.getSourcePort(), numTransfers);
				} finally {
					l.unlock();
				}
				numMessages++;
				offset = numMessages * actualChunkSize;
			}
		}
		System.out.println("Mensagens de pedido de transferÊncia terminaram");
		return true;
	}*/

	public void manageFileAnswers(PDU p){
		/*long filesSize = 0;

		try{
			l.lock();
			filesSize = files.size();
		} finally {
			l.unlock();
		}
		if(filesSize == 0){
			files.put
		}*/
	}

	public void receiveFFS(){
		new Thread(() -> {
			while (true) {
				PDU p = FSChunkProtocol.receivePacket(s);

				int type = p.getType();
				switch (type) {
					case 1:
						//System.out.println("..");
						manageServer(p);
						break;
					case 3:
						// Receber respostas dos servidores acerca da existência do ficheiro
						System.out.println("3");
						byte[] data = p.getData();
						int datasize = data.length;
						long size = ByteBuffer.wrap(data,0,Long.BYTES).getLong();
						byte[] filename = new byte[datasize - Long.BYTES];
						System.arraycopy(data,Long.BYTES,filename,0,datasize-Long.BYTES);
						filename = removeTrash(filename);
						String file = new String(filename);
						System.out.println("Tamanho do ficheiro: " + size + " ficheiro -> " + file);
						//manageFileAnswers(p);
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
