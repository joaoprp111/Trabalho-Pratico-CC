import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FFS {
    public static void main(String[] args) {
        try {
            DatagramSocket s = new DatagramSocket();
            InetAddress ip = InetAddress.getLocalHost();

            byte[] enviar;
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(1);
            enviar = bb.array();
            byte[] str = "beacon".getBytes();
            byte[] msg = new byte[enviar.length + str.length];
            System.arraycopy(enviar,0,msg,0,enviar.length);
            System.arraycopy(str,0,msg,enviar.length,str.length);

            DatagramPacket packet = new DatagramPacket(msg, msg.length,ip,4472);
            s.send(packet);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
