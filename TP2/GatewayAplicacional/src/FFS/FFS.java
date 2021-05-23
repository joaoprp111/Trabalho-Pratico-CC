package FFS;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
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

    private byte[] removeTrash(byte[] source){
        byte[] dest;
        int i = 0;
        while(source[i] != 0){
            i++;
        }
        dest = new byte[i];
        System.arraycopy(source,0,dest,0,i);
        return dest;
    }

    private String filePath(byte[] filename, String absolutPath){
        byte[] cleanFilename = removeTrash(filename);
        String cleanFilenameStr = new String(cleanFilename);
        StringBuilder sb = new StringBuilder(absolutPath);
        sb.append(this.targetFilesDir).append(cleanFilenameStr);
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
                int type = p.getType();
                switch(type){
                    case 2:
                        response(p);
                        break;
                    case 4:
                        byte[] data = p.getData();
                        System.out.println(data);
                        break;
                }
            }
        }).start();
    }

    public void response(PDU p){
        String absolutPath = System.getProperty("user.dir");
        String path = filePath(p.getData(),absolutPath);
        File file = new File(path);
        try{
            Scanner sc = new Scanner(file);
            System.out.println("O ficheiro " + path + " existe!");
            FSChunkProtocol.sendResponse(this.s,file,this.ip,this.destPort);
        } catch(FileNotFoundException e){
            System.out.println("Ficheiro não existe!");
        }
    }

    public static void main(String[] args) {
        FFS ffs = new FFS();

        ffs.runServer();
    }
}
