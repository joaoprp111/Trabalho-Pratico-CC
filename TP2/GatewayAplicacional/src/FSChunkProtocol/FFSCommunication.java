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
                int size = ByteBuffer.wrap(data,0,4).getInt();
                int type = ByteBuffer.wrap(data,4,4).getInt();
                byte[] content = new byte[size-8];
                System.arraycopy(data,8,content,0,size-8);

                String contentstr = new String(content);
                System.out.println("String size " + content.length);
                System.out.println("Size:" + size);
                System.out.println("Type:" + type);
                System.out.println("Message: " +  contentstr);
                running = false;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
