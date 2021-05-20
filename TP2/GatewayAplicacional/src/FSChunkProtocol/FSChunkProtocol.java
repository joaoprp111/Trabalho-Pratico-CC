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

            if(timeSeconds > 2.5) {
                sendPacket(s, data, ip, destPort);
                System.out.println("> FFS: Beacon enviada!");
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
            System.out.println("> Gw: beacon recebida da porta " + pckt.getPort());
        } catch(IOException e){
            e.printStackTrace();
        }

        int packetSize = pckt.getLength();
        int type = ByteBuffer.wrap(data,0,4).getInt();
        res.setType(type);

        // Tratar do resto do conteúdo
        if(type == 1){
            // Beacon

            // Ler porta
           int srcPort = pckt.getPort();
           ByteBuffer bb = ByteBuffer.allocate(4);
           bb.putInt(srcPort);
           byte[] srcPortArr = bb.array();

           // Ler Ip
           byte[] srcIp = pckt.getAddress().getAddress();

           // Preencher array
           byte[] pduData = new byte[packetSize-4+4+srcIp.length];
           System.arraycopy(srcPortArr,0,pduData,0,4);
           System.arraycopy(srcIp,0,pduData,4,srcIp.length);

           res.setData(pduData);
        }

        return res;
    }
}
