import FSChunkProtocol.PDU;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FFS {
    public static void main(String[] args) {
        try {
            //Criar um socket
            DatagramSocket s = new DatagramSocket();
            InetAddress ip = InetAddress.getLocalHost();

            //Criar um packet com as regras do protocolo FSChunk
            PDU pdu = new PDU();

            //Criar uma thread para as beacons e outra para tratar os pedidos vindos do gateway







            //Array com o tipo
            byte[] enviar;
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(1);
            enviar = bb.array();

            // Array com a mensagem (conteúdo)
            byte[] str = "beacon".getBytes();

            // Array final com tudo
            byte[] msg = new byte[enviar.length + str.length + 4];

            //Array com o tamanho
            byte[] arraySize;
            ByteBuffer bb2 = ByteBuffer.allocate(4);
            bb2.putInt(enviar.length + str.length + 4);
            arraySize = bb2.array();

            //Copiar o tamanho para o array final
            System.arraycopy(arraySize,0,msg,0,arraySize.length);

            //Copiar o tipo
            System.arraycopy(enviar,0,msg,arraySize.length,enviar.length);

            //Copiar o conteúdo
            System.arraycopy(str,0,msg,arraySize.length + enviar.length,str.length);

            System.out.println("Tamanho do array: " + msg.length);

            DatagramPacket packet = new DatagramPacket(msg, msg.length,ip,4472);
            s.send(packet);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
