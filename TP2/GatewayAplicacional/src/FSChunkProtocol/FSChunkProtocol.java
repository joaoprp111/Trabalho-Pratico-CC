package FSChunkProtocol;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.String;

public class FSChunkProtocol{

    public static void sendTransferRequest(DatagramSocket s, String file, long offset, long chunkSize, InetAddress ip, int port, int transferId){
        byte[] offsetArr = PDU.conversionFromLong(offset);
        byte[] fileArr = file.getBytes();
        byte[] chunk = PDU.conversionFromLong(chunkSize);
        byte[] finalData = new byte[offsetArr.length + fileArr.length + chunk.length];
        System.arraycopy(offsetArr,0,finalData,0,offsetArr.length);
        System.arraycopy(chunk,0,finalData,offsetArr.length,chunk.length);
        System.arraycopy(fileArr,0,finalData,offsetArr.length + chunk.length, fileArr.length);
        PDU p = new PDU(4,finalData,transferId);
        byte[] packetData = p.serialize();
        sendPacket(s,packetData,ip,port);
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

            if(timeSeconds > 10) {
                sendPacket(s,data,ip,destPort);
                lastTime = System.nanoTime();
            }
        }
    }

    public static PDU receivePacket(DatagramSocket s){
        byte[] buffer = new byte[2048];
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

    public static void sendResponse(DatagramSocket s, File file, byte[] filename, InetAddress ip, int port){
        PDU p = new PDU(3); // Resposta ao pedido do ficheiro
        byte[] fileSize = PDU.conversionFromLong(file.length());
        byte[] packetData = new byte[fileSize.length + filename.length];
        System.arraycopy(fileSize,0,packetData,0,fileSize.length);
        System.arraycopy(filename,0,packetData,fileSize.length,filename.length);
        p.setData(packetData);
        byte[] data = p.serialize();
        sendPacket(s,data,ip,port);
    }

    public static void sendChunkPacket(DatagramSocket s, byte[] offset, byte[] chunkSize, byte[] chunk, byte[] filename, InetAddress ip, int port){
        PDU p = new PDU(5); // Transferência de um chunk
        byte[] data = new byte[offset.length + chunkSize.length + chunk.length + filename.length];
        System.arraycopy(offset,0,data,0,offset.length);
        System.arraycopy(chunkSize,0,data,offset.length,chunkSize.length);
        System.arraycopy(chunk,0,data,offset.length + chunkSize.length,chunk.length);
        System.arraycopy(filename,0,data,offset.length + chunkSize.length + chunk.length,filename.length);

        p.setData(data);
        byte[] allPacketData = p.serialize();
        sendPacket(s,allPacketData,ip,port);
    }
}
