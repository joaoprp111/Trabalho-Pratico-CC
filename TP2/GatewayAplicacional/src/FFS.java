import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class FFS {
    public static void main(String[] args) {
        try {
            DatagramSocket s = new DatagramSocket();
            InetAddress ip = InetAddress.getLocalHost();

            byte[] enviar;
            String str = "beacon from FFS";
            enviar = str.getBytes(StandardCharsets.UTF_8);

            DatagramPacket pedido = new DatagramPacket(enviar, enviar.length,ip,9876);
            s.send(pedido);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
