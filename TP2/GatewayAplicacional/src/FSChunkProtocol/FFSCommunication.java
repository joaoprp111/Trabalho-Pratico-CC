package FSChunkProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class FFSCommunication implements Runnable{
    private DatagramSocket socket;
    private boolean running;
    private PDU packet;
    private byte[] buffer = new byte[4*1024];

    public FFSCommunication(){
        try{
            socket = new DatagramSocket(4472);
        } catch(SocketException se){
            se.printStackTrace();
        }
        running = false;
    }

    public void run(){
        running = true;

        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, 4*1024);
                socket.receive(packet);
                byte[] data = packet.getData();
                int typeInt = ByteBuffer.wrap(data,0,4).getInt();
                System.out.println("Data length:" + data.length);
                System.out.println("Type:" + typeInt);
                running = false;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
