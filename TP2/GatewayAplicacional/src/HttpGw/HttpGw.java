package HttpGw;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class HttpGw {
	private DatagramSocket s;
	private ServerSocket ss;
	private Map<String,Connection> connections; // Chave -> porta + "-" + ip
    private Map<String,File> files;
	private int numTransfers;
	private Lock l;

	public HttpGw() {
		try {
			this.s = new DatagramSocket(8080);
			this.connections = new HashMap<>();
			this.l = new ReentrantLock();
			this.ss = new ServerSocket(8080);
			this.numTransfers = 0;
			this.files = new HashMap<>();
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
            files.put(filename,new File(null,0,-1));
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
			   System.out.println("O servidor com ip e porta " + par.getKey() + " desconectou-se.");
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

	public void manageFileAnswers(PDU p){

		// Descodificar a mensagem
		byte[] data = p.getData();
		long fileSize = ByteBuffer.wrap(data,0,Long.BYTES).getLong();
		byte[] fileArr = new byte[data.length - Long.BYTES];
		System.arraycopy(data,Long.BYTES,fileArr,0,data.length-Long.BYTES);
		fileArr = removeTrash(fileArr);
		String filename = new String(fileArr);
		InetAddress ip = p.getIp();
		int port = p.getPort();

		// Alterar a informação da conexão
		StringBuilder sb = new StringBuilder(String.valueOf(port));
		sb.append("-");
		sb.append(ip.toString());
		String key = sb.toString();
		Connection c;
		try{
			l.lock();
			c = connections.get(key);
			c.setCurrentFileTransfer(filename);
			c.setCurrentFileSize(fileSize);
			File f = files.get(filename);
			f.setSize((int)fileSize);
			files.replace(filename,f);
		} finally {
			l.unlock();
		}
	}

	public void manageTransfers(PDU p){
	    byte[] allData = p.getData();
	    long offset = ByteBuffer.wrap(allData,0,Long.BYTES).getLong();
	    long chunkSize = ByteBuffer.wrap(allData,Long.BYTES,Long.BYTES).getLong();
	    byte[] chunk = new byte[(int)chunkSize];
	    byte[] filename = new byte[allData.length - (2*Long.BYTES + chunk.length)];
	    System.arraycopy(allData,Long.BYTES * 2,chunk,0,chunk.length);
	    Collection<Connection> cs;
	    System.arraycopy(allData,(Long.BYTES * 2)+(int)chunkSize,filename,0,allData.length - (2*Long.BYTES + chunk.length));
	    filename = removeTrash(filename);
	    String filenameStr = new String(filename);

	    try{
	        l.lock();
	        File f = files.get(filenameStr);
	        f.writeInArray(offset,chunk.length,chunk);
        } finally {
	        l.unlock();
        }
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
						manageFileAnswers(p);
						break;
					case 5:
					    // Receber transferências de chunks
                        manageTransfers(p);
					default:
						break;
				}
			}
		}).start();
	}

	public void requestChunks(int servers, Collection<Connection> cs, int transferId, String filename){
		int requests = 0;
		long fileSize = 1000000000; // Começar com um nº grande para entrar no ciclo
		long offset = 0, chunkSize = 0;
		Iterator<Connection> it = cs.iterator();
		while(offset < fileSize){
			if(!it.hasNext()){
				it = cs.iterator();
			}
			Connection c = it.next();
			if(requests == 0){
				try{
					c.lock();
					fileSize = c.getCurrentFileSize();
				} finally {
					c.unlock();
				}
				chunkSize = (fileSize / servers) > 512 ? 512 : (fileSize / servers);
			}
			try {
			    if(offset + chunkSize > fileSize)
			        chunkSize = fileSize - offset;
				c.lock();
				FSChunkProtocol.sendTransferRequest(s, c.getCurrentFileTransfer(), offset, chunkSize,
						c.getSourceIp(), c.getSourcePort(),transferId);
			} finally {
				c.unlock();
			}
			requests++;
			offset += chunkSize;
		}
	}

	public boolean transfer(String filename, int transferId){
		Collection<Connection> availableConnections;
		try{
			l.lock();
			availableConnections = connections.values().stream()
					.filter(c -> c.getCurrentFileTransfer().equals(filename))
					.collect(Collectors.toCollection(ArrayList::new));
		} finally {
			l.unlock();
		}
		int size = availableConnections.size();
		if(size == 0)
			return false;
		else {
			requestChunks(size, availableConnections, transferId, filename);
			return true;
		}
	}

	public void receiveClients(){
		// Thread que aceita HTTP Requests dos clientes
		new Thread(() -> {
			try {
				while (true) {
					Socket socket = ss.accept();
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream());

					String input;
					if((input = in.readLine()) != null) {
						String filename = (input.split(" ")[1]).split("/")[1];
						boolean transferCompleted = false;
						double requestTime = (double) System.nanoTime() / 1000000000;
						double currentTime = (double) System.nanoTime() / 1000000000;
						while(!transferCompleted && (currentTime - requestTime) < 30) {
							requestFileData(filename);
							try {
								sleep(5000);
							} catch (Exception e) {
								e.printStackTrace();
							}
							if (!transfer(filename, numTransfers)) {
								out.println("HTTP/1.1 404 File Not Found");
								out.println("Server: Java HTTP Server from HttpGw");
								out.println("Date: " + new Date());
								out.println("Content-type: " + filename);
								out.println("Content-length: " + 0);
								out.println();
								out.flush();
							} else {
								try {
									sleep(5000);
								} catch (Exception e) {
									e.printStackTrace();
								}
								try {
									l.lock();
									File f = files.get(filename);
									if (f.getBytesWritten() == f.getSize()) {
										transferCompleted = true;
										System.out.println("Transferência do ficheiro " + filename + " completa");
										out.println("HTTP/1.1 200 OK");
										out.println("Server: Java HTTP Server from HttpGw");
										out.println("Date: " + new Date());
										out.println("Content-type: " + filename);
										out.println("Content-length: " + f.getSize());
										out.println();
										out.flush();

										dataOut.write(f.getFileRebuild(), 0, f.getSize());
										dataOut.flush();

										numTransfers++;
										socket.shutdownOutput();
										socket.shutdownInput();
										socket.close();
									}
								} finally {
									l.unlock();
								}
							}
							currentTime = (double) System.nanoTime() / 1000000000;
						}
						if(!transferCompleted) {
							System.out.println("O tempo de espera acabou.");
							out.println("HTTP/1.1 509 Couldn't transfer file");
							out.println("Server: Java HTTP Server from HttpGw");
							out.println("Date: " + new Date());
							out.println("Content-type: " + filename);
							out.println("Content-length: " + 0);
							out.println();
							out.flush();
						}
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
		InetAddress ip = null;
		try{
		    ip = InetAddress.getLocalHost();
        } catch(Exception e){
		    try{
		        ip = InetAddress.getByName("10.1.1.1");
            } catch(Exception e2){
		        e2.printStackTrace();
            }
        }
		System.out.println("Ativo em " + ip.toString() + " na porta 8080");

		gw.runGateway();
	}
}
