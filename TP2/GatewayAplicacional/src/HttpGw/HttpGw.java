package HttpGw;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HttpGw {
    private DatagramSocket s;
    private ServerSocket ss;
    private Map<String,Connection> connections; // Chave -> porta + "-" + ip
    private Lock l;
    private final long _10e9 = 1000000000;

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

    private void manageServer(PDU p){
        byte[] data = p.getData();
        int size = data.length;
        int port = ByteBuffer.wrap(data,0,4).getInt();
        byte[] ipArr = new byte[size-4];
        System.arraycopy(data,4,ipArr,0,size-4);
        try {
            InetAddress ip = InetAddress.getByAddress(ipArr);

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
                    double nextBeaconSeconds = (double) System.nanoTime() / _10e9;
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
                    while((input = in.readLine()) != null)
                        System.out.println(input);
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
