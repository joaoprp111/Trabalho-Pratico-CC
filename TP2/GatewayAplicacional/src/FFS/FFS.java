package FFS;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.File;
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

        new Thread(() -> {
            while(true) {
                PDU p = FSChunkProtocol.receivePacket(s);
                System.out.println(p);
                response(p);
            }
        }).start();
    }

    public void response(PDU p){
        File file = new File(new String(p.getData()));
        if(file.exists()){
            FSChunkProtocol.sendResponse(this.s,file);
        }
    }

    public static void main(String[] args) {
        FFS ffs = new FFS();

        ffs.runServer();
    }
}
