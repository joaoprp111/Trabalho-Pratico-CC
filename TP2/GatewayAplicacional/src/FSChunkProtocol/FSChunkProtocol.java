package FSChunkProtocol;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;

public class FSChunkProtocol{
    private FFSCommunication ffsWorker;
    private HttpGwCommunication gwWorker;
    private boolean running;
    private byte[] content = new byte[256];

    public FSChunkProtocol(){
        ffsWorker = new FFSCommunication();
        gwWorker = new HttpGwCommunication();
        running = false;
    }

    public static void main(String[] args){

        //Vai ficar a ouvir, à espera de algum contacto por parte de um Fast File Server
        new Thread(new FFSCommunication()).start();
    }
    //Protocolo implementado, vai ter acesso ao FSChunkProtocol.PDU
}
