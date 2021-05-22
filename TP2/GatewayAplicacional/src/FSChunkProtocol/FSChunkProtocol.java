package FSChunkProtocol;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.String;

public class FSChunkProtocol{

    public static void sendTransferRequest(DatagramSocket s, String file, long offset, long chunkSize, InetAddress ip, int port, int transferId){

    }

    public static void sendMetaDataRequest(DatagramSocket s,String file, InetAddress ip, int port){
        byte[] fileArr = file.getBytes();
        PDU p = new PDU(2,fileArr);
        byte[] data = p.serialize();
        sendPacket(s,data,ip,port);
    }

    public static void sendPacket(DatagramSocket s,byte[] data, InetAddress ip, int port){
        DatagramPacket pckt = new DatagramPacket(data,data.length,ip,port);
        try {
            s.send(pckt);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void sendBeacons(DatagramSocket s, InetAddress ip, int destPort){
        PDU p = new PDU(1);
        byte[] data = p.serialize();
        sendPacket(s,data,ip,destPort);
        long lastTime = System.nanoTime();
        while(true){
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - lastTime;
            double timeSeconds = (double) elapsedTime / 1000000000;

            if(timeSeconds > 2.5) {
                sendPacket(s,data,ip,destPort);
                lastTime = System.nanoTime();
            }
        }
    }

    public static PDU receivePacket(DatagramSocket s){
        byte[] buffer = new byte[256];
        PDU p = new PDU();

        DatagramPacket pckt = new DatagramPacket(buffer,buffer.length);

        try {
            s.receive(pckt);
        } catch(IOException e){
            e.printStackTrace();
        }

        p.desserialize(pckt);

        return p;
    }

    public static void sendResponse(DatagramSocket s, File file, InetAddress ip, int port){
        PDU p = new PDU(3); // Resposta ao pedido do ficheiro
        long fileSize = file.length();
        p.setData(fileSize);
        byte[] data = p.serialize();
        sendPacket(s,data,ip,port);
    }
}
