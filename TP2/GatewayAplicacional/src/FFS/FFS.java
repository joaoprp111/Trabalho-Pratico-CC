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
    private final String targetFilesDir = "/files/";

    public FFS(){
        try{
            s = new DatagramSocket();
            ip = InetAddress.getLocalHost();
            destPort = 8080;
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private String filesPath(String filename, String absolutPath){
        StringBuilder sb = new StringBuilder(absolutPath);
        sb.append(this.targetFilesDir).append(filename);
        return sb.toString();
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
        String absolutPath = System.getProperty("user.dir");
        String filename = new String(p.getData());
        String path = filesPath(filename,absolutPath);
        File file = new File(path);
        if(file.exists()){
            System.out.println("O ficheiro " + path + " existe!");
            FSChunkProtocol.sendResponse(this.s,file,this.ip,this.destPort);
        }
        else{
            System.out.println("O ficheiro " + path + " não existe!");
        }
    }

    public static void main(String[] args) {
        FFS ffs = new FFS();

        ffs.runServer();
    }
}
