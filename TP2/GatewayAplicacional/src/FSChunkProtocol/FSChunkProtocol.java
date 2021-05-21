package FSChunkProtocol;

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
    private static int id = 0;
    private static Lock l = new ReentrantLock();

    private static byte[] conversion(int x){
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(x);
        return bb.array();
    }

    private static int nextId(){
        try {
            l.lock();
            id = id + 1;
        } finally {
            l.unlock();
        }
        return id;
    }

    public static byte[] serialize(PDU p){
        byte[] data = p.getData();
        byte[] content;
        if(data != null)
            content = new byte[4 * 5 + data.length];
        else
            content = new byte[4 * 5];
        byte[] type = conversion(p.getType());
        byte[] transferId = conversion(p.getTransferId());
        byte[] checksum = conversion(p.getChecksum());
        byte[] offset = conversion(p.getOffset());
        byte[] packetId = conversion(p.getPacketId());

        System.arraycopy(type,0,content,0,4);
        System.arraycopy(transferId,0,content,4,4);
        System.arraycopy(checksum,0,content,8,4);
        System.arraycopy(offset,0,content,12,4);
        System.arraycopy(packetId,0,content,16,4);
        if(data != null)
            System.arraycopy(data,0,content,20,data.length);

        return content;
    }

    public static PDU desserialize(byte[] content, InetAddress ip, int port) {

        PDU res = new PDU();
        int size = content.length;
        int type = ByteBuffer.wrap(content, 0, 4).getInt();
        int transferId = ByteBuffer.wrap(content, 4, 4).getInt();
        int checksum = ByteBuffer.wrap(content, 8, 4).getInt();
        int offset = ByteBuffer.wrap(content, 12, 4).getInt();
        int packetId = ByteBuffer.wrap(content, 16, 4).getInt();
        res.setType(type);
        res.setIp(ip);
        res.setPort(port);
        res.setTransferId(transferId);
        res.setChecksum(checksum);
        res.setOffset(offset);
        res.setPacketId(packetId);
        byte[] data = null;
        if(size > (4 * 5)) {
            int restSize = size - (4*5);
            data = new byte[restSize];
            System.arraycopy(content,4*5,data,0,restSize);
        }
        res.setData(data);

        return res;
    }

    public static void sendMetaDataRequest(DatagramSocket s,String file, InetAddress ip, int port){
        byte[] fileArr = file.getBytes();
        PDU p = new PDU(2,nextId(),fileArr);
        byte[] data = serialize(p);
        sendPacket(s,data,ip,port);
    }

    public static void sendPacket(DatagramSocket s,byte[] data,InetAddress ip, int destPort){
        DatagramPacket pckt = new DatagramPacket(data,data.length,ip,destPort);
        try {
            s.send(pckt);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void sendBeacons(DatagramSocket s, InetAddress ip, int destPort){

        PDU p = new PDU(1,nextId());
        byte[] data = serialize(p);
        sendPacket(s,data,ip,destPort);
        long lastTime = System.nanoTime();
        while(true){
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - lastTime;
            double timeSeconds = (double) elapsedTime / 1000000000;

            if(timeSeconds > 2.5) {
                PDU p2 = new PDU(1,nextId());
                byte[] data2 = serialize(p2);
                sendPacket(s, data2, ip, destPort);
                System.out.println("> FFS: Beacon enviada!");
                lastTime = System.nanoTime();
            }
        }
    }

    public static PDU receivePacket(DatagramSocket s){

        byte[] buffer = new byte[256];

        DatagramPacket pckt = new DatagramPacket(buffer,buffer.length);

        try {
            s.receive(pckt);
            System.out.println("> Gw: beacon recebida da porta " + pckt.getPort());
        } catch(IOException e){
            e.printStackTrace();
        }

        return desserialize(pckt.getData(),pckt.getAddress(),pckt.getPort());
    }
}
