package HttpGw;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class HttpGw {
    private DatagramSocket s;
    private Map<String,Connection> connections;

    public HttpGw() {
        try {
            this.s = new DatagramSocket(4475);
            this.connections = new HashMap<>();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void registerServer(PDU p){
    }

    public void runGateway(){
        new Thread(() -> {
            while(true) {
                PDU p = FSChunkProtocol.receivePacket(s);

                int type = p.getType();
                /*byte[] data = p.getData();
                System.out.println(type);
                System.out.println(data.toString());*/
                switch(type){
                    case 1:
                        registerServer(p);
                }
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
