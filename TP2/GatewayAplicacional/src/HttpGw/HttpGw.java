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
	private DatagramSocket s; // Socket utilizado para a transmissão UDP (com os Fast File Servers)
	private ServerSocket ss; // Socket utilizado para a transmissão TCP (com o cliente)
	private Map<String,Connection> connections; // Chave -> porta + "-" + ip
    private Map<String,File> files; // Map que guarda informações sobre os ficheiros que estão a ser transferidos
	private int numTransfers; // Número de transferências completas
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

    /**
     * Remover os bytes a mais no array de bytes
     * @param source array de bytes
     * @return       array de bytes sem o 'lixo'
     */
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

    /**
     * Solicitar os metadados relativos a um ficheiro, este pedido é enviado a todos os servidores conectados
      * @param filename Nome do ficheiro que o cliente quer que seja transferido
     */
   public void requestFileData(String filename){
		Collection<String> inactives = new ArrayList<>();
		Set<Map.Entry<String,Connection>> cs;

		// Obter os servidores conectados e adicionar o ficheiro ao map dos ficheiros
		try{
			l.lock();
			cs = connections.entrySet();
            files.put(filename,new File(null,0,-1));
		} finally {
			l.unlock();
		}

		// Percorrer cada servidor
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

		   // Verificar se deixou de mandar beacons há mais de 30 segundos
		   if ((currentTime - connectionLastBeacon) > 30) {
			   //Desconectou, temos de remover
			   inactives.add(par.getKey());
			   System.out.println("O servidor com ip e porta " + par.getKey() + " desconectou-se.");
		   } else {
			   // Enviar o pedido dos metadados ao servidor
			   FSChunkProtocol.sendMetaDataRequest(s,filename,destIp,destPort);
		   }
	    }

	    // Percorrer os servidores novamente e remover os que já deixaram de mandar beacons há mais de 30 segundos
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

    /**
     * Gestão dos registos dos servidores no gateway, se não estiver registado o gateway regista-o,
     * e altera o tempo da última beacon recebida
     * @param p Pacote do tipo 1 (beacon)
     */
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

    /**
     * Tratar da resposta relativa ao pedido prévio dos metadados do ficheiro
     * @param p Pacote do tipo 3 (resposta com os metadados do ficheiro)
     */
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

    /**
     * Gestão dos chunks que chegam, e contêm pedaços do ficheiro pedido anteriormente
     * @param p Pacote do tipo 5 (transferência de chunks)
     */
	public void manageTransfers(PDU p){

	    // Descodificar o pacote
	    byte[] allData = p.getData();
	    long offset = ByteBuffer.wrap(allData,0,Long.BYTES).getLong();
	    long chunkSize = ByteBuffer.wrap(allData,Long.BYTES,Long.BYTES).getLong();
	    byte[] chunk = new byte[(int)chunkSize];
	    byte[] filename = new byte[allData.length - (2*Long.BYTES + chunk.length)];
	    System.arraycopy(allData,Long.BYTES * 2,chunk,0,chunk.length);
	    System.arraycopy(allData,(Long.BYTES * 2)+(int)chunkSize,filename,0,allData.length - (2*Long.BYTES + chunk.length));
	    filename = removeTrash(filename);
	    String filenameStr = new String(filename);

	    // Escrever o chunk no byte array destinado para reconstruir este ficheiro
	    try{
	        l.lock();
	        File f = files.get(filenameStr);
	        f.writeInArray(offset,chunk.length,chunk);
        } finally {
	        l.unlock();
        }
    }

    /**
     * Thread responsável por receber as mensagens vindas dos Fast File Servers
     */
	public void receiveFFS(){
		new Thread(() -> {
			while (true) {
				PDU p = FSChunkProtocol.receivePacket(s);

				int type = p.getType();
				switch (type) {
					case 1:
						// Receber beacons dos servidores e verificar se já está registado na tabela com ip e porta
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

    /**
     * Calcular o tamanho de cada chunk, decidir a quantos servidores pedir, e efetivamente pedir os chunks
     * @param servers Número de servidores que contêm o ficheiro e se encontram conectados ao gateway
     * @param cs      Coleção com as conexões (servidores) que vamos usar para pedir os chunks
     * @param transferId Identificador da transferência em causa
     * @param filename   Nome do ficheiro a transferir
     */
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
			// Se for o primeiro pedido, vamos calcular o tamanho dos chunks, que será sempre menor ou igual a 512 bytes
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
				// Enviar o pedido do chunk
				FSChunkProtocol.sendTransferRequest(s, c.getCurrentFileTransfer(), offset, chunkSize,
						c.getSourceIp(), c.getSourcePort(),transferId);
			} finally {
				c.unlock();
			}
			requests++;
			offset += chunkSize;
		}
	}

    /**
     * Iniciar uma transferência
     * @param filename Ficheiro a ser transferido
     * @param transferId Id da transferência
     * @return          Booleano que indica se a transferência inicou corretamente ou não
     */
	public boolean transfer(String filename, int transferId){
		Collection<Connection> availableConnections;
		try{
		    // Obter todas as conexões disponíveis para transferir o ficheiro
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
		    // Pedir os chunks
			requestChunks(size, availableConnections, transferId, filename);
			return true;
		}
	}

    /**
     * Thread responsável por atender os pedidos HTTP dos clientes
     */
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
						// O gateway pede retransmissão caso não consiga completar a transferência no primeiro pedido
                        // Este pedido é feito em ciclo até a transferência ser efetuada
                        // ou o tempo de espera exceder 30 segundos
						while(!transferCompleted && (currentTime - requestTime) < 30) {
						    // Pedir metadados
							requestFileData(filename);

							// Esperar 5 segundos pelas respostas
							try {
								sleep(5000);
							} catch (Exception e) {
								e.printStackTrace();
							}

							// Responder ao cliente
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

    /**
     * Ativar o gateway e as suas threads
     */
	public void runGateway() {
		receiveFFS();

		receiveClients();
	}

    /**
     * Main do gateway
     * @param args Não são passados argumentos
     */
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
