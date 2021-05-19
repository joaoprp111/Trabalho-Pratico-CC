package FSChunkProtocol;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class FSChunkProtocol{

    public static void sendPacket(DatagramSocket s,byte[] data,InetAddress ip, int destPort){
        DatagramPacket pckt = new DatagramPacket(data,data.length,ip,destPort);
        try {
            s.send(pckt);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void sendBeacons(DatagramSocket s, InetAddress ip, int destPort){

        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(1); //Tipo da beacon
        byte[] data = bb.array();
        sendPacket(s,data,ip,destPort);
        long lastTime = System.nanoTime();
        while(true){
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - lastTime;
            double timeSeconds = (double) elapsedTime / 1000000000;

            if(timeSeconds > 2.0) {
                sendPacket(s, data, ip, destPort);
                System.out.println("Enviado!");
                lastTime = System.nanoTime();
            }
        }
    }

    public static PDU receivePacket(DatagramSocket s){
        PDU res = new PDU();
        byte[] buffer = new byte[256];

        DatagramPacket pckt = new DatagramPacket(buffer,buffer.length);
        byte[] data = pckt.getData();

        try {
            s.receive(pckt);
            System.out.println("Recebido no Gateway!");
        } catch(IOException e){
            e.printStackTrace();
        }

        int packetSize = pckt.getLength();
        int type = ByteBuffer.wrap(data,0,4).getInt();
        res.setType(type);

        // Tratar do resto do conteúdo
        byte[] separator = "-".getBytes();
        byte[] srcIp = pckt.getAddress().getAddress();
        int srcPort = pckt.getPort();
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(srcPort);
        byte[] srcPortArray = bb.array();

        int finalSize = (packetSize > 4) ? (srcIp.length + 1 + 4 + 1 + packetSize - 4) : (srcIp.length + 1 + 4);
        byte[] restData = new byte[finalSize];
        System.arraycopy(srcIp,0,restData,0,srcIp.length);
        System.arraycopy(separator,0,restData,srcIp.length,1);
        System.out.println("Tamanho do ip em bytes: " + srcIp.length);
        System.arraycopy(srcPortArray,0,restData,srcIp.length+1,4);
        if(packetSize > 4) {
            System.arraycopy(separator, 0, restData, srcIp.length + 1 + 4, 1);
            System.arraycopy(data, 4, restData, srcIp.length + 1 + 4 + 1, packetSize - 4);
        }
        res.setData(restData);

        return res;
    }
}
