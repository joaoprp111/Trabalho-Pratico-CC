package HttpGw;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HttpGw {
    private DatagramSocket s;
    private Map<String,Connection> connections; // Chave -> porta + "-" + ip
    private Lock l;
    private final long _10e9 = 1000000000;

    public HttpGw() {
        try {
            this.s = new DatagramSocket(4475);
            this.connections = new HashMap<>();
            this.l = new ReentrantLock();
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

    public void runGateway() {
        // Thread que recebe pacotes
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

    public static void main(String[] args){
        HttpGw gw = new HttpGw();

        gw.runGateway();
    }
}
