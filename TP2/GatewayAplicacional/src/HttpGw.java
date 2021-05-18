import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class HttpGw {
    //Criar uma thread por FFS
    //Ter acesso ao FSChunk

    /*try {
        DatagramSocket s = new DatagramSocket(9876);

        byte[] receber = new byte[1924];

        DatagramPacket pedido = new DatagramPacket(receber, receber.length);
        s.receive(pedido);

        int i = 0;
        StringBuilder pedidoStr = new StringBuilder();
        while(receber[i] != 0){
            pedidoStr.append((char) receber[i]);
            i++;
        }
        System.out.println("Recebido do FFS: " + pedidoStr);
    } catch(Exception e){
        e.printStackTrace();
    }*/
}
