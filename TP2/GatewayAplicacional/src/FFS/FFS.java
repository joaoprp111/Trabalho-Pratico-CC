package FFS;

import FSChunkProtocol.FSChunkProtocol;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FFS {
    private DatagramSocket s;
    private InetAddress ip;
    private int destPort;

    public FFS(){
        try{
            s = new DatagramSocket();
            ip = InetAddress.getLocalHost();
            destPort = 8080;
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void runServer(){

        //Criar uma thread para as beacons e outra para tratar os pedidos vindos do gateway
        new Thread(() -> {
            FSChunkProtocol.sendBeacons(s,ip,destPort);
        }).start();

        FSChunkProtocol.receivePacket(s);
    }

    public static void main(String[] args) {
        FFS ffs = new FFS();

        ffs.runServer();
    }
}
