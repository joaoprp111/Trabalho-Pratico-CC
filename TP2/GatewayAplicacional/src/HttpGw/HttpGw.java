package HttpGw;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;
import java.net.DatagramSocket;

public class HttpGw {
    private DatagramSocket s;

    public HttpGw() {
        try {
            this.s = new DatagramSocket(4475);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void runGateway(){
        new Thread(() -> {
            while(true) {
                PDU p = FSChunkProtocol.receivePacket(s);
                System.out.println(p.getType());
            }
        }).start();
    }

    /*try {
        DatagramSocket s = new DatagramSocket(80);

        byte[] receber = new byte[1924];

        DatagramPacket pedido = new DatagramPacket(receber, receber.length);
        s.receive(pedido);

        int i = 0;
        StringBuilder pedidoStr = new StringBuilder();
        while(receber[i] != 0){
            pedidoStr.append((char) receber[i]);
            i++;
        }
        System.out.println("Recebido do FFS.FFS: " + pedidoStr);
    } catch(Exception e){
        e.printStackTrace();
    }*/

    public static void main(String[] args){
        HttpGw gw = new HttpGw();

        gw.runGateway();
    }
}
