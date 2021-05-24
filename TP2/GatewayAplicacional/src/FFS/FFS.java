package FFS;

import FSChunkProtocol.FSChunkProtocol;
import FSChunkProtocol.PDU;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
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

    public FFS(String gatewayIp, String gatewayPort){
        try{
            s = new DatagramSocket();
            ip = InetAddress.getByName(gatewayIp);
            destPort = Integer.parseInt(gatewayPort);
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

    private String filePath(byte[] cleanFilename, String absolutPath){
        String cleanFilenameStr = new String(cleanFilename);
        StringBuilder sb = new StringBuilder(absolutPath);
        sb.append(this.targetFilesDir).append(cleanFilenameStr);
        return sb.toString();
    }

    public void runServer(){

        //Criar uma thread para as beacons e outra para tratar os pedidos vindos do gateway
        new Thread(() -> {
            FSChunkProtocol.sendBeacons(s, ip, destPort);
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
                        sendChunk(p);
                        break;
                }
            }
        }).start();
    }

    public void response(PDU p){
        byte[] filename = removeTrash(p.getData());
        String absolutPath = System.getProperty("user.dir");
        String path = filePath(filename,absolutPath);
	System.out.println("Caminho do ficheiro: " + path);
        File file = new File(path);
        try{
            Scanner sc = new Scanner(file);
            System.out.println("O ficheiro " + path + " existe!");
            FSChunkProtocol.sendResponse(this.s,file,filename,this.ip,this.destPort);
        } catch(FileNotFoundException e){
            System.out.println("Ficheiro não existe!");
        }
    }

    public byte[] readFromFile(String path,int offset, int size){
        byte[] content = new byte[size];
        int numBytesRead = 0;
        try {
            RandomAccessFile f = new RandomAccessFile(path,"r");
            f.seek(offset);
            numBytesRead = f.read(content,0,size);
            System.out.println("Bytes read: " + numBytesRead);
        } catch(Exception e){
            e.printStackTrace();
        }
        return content;
    }

    public void sendChunk(PDU p){
        // Obter a informação necessária
        byte[] data = p.getData();
        byte[] offsetArr = new byte[Long.BYTES];
        System.arraycopy(data,0,offsetArr,0,Long.BYTES);
        byte[] chunkArr = new byte[Long.BYTES];
        System.arraycopy(data,Long.BYTES,chunkArr,0,Long.BYTES);
        long offset = ByteBuffer.wrap(data,0,Long.BYTES).getLong();
        long chunk = ByteBuffer.wrap(data,Long.BYTES,Long.BYTES).getLong();
        byte[] filename = new byte[data.length - (2* Long.BYTES)];
        System.arraycopy(data,2*Long.BYTES,filename,0,data.length-(2*Long.BYTES));
        filename = removeTrash(filename);
        String file = new String(filename);
        System.out.println("Offset: " + offset +
                " | Chunk: " + chunk + " | Filename: " + file);

        // Ler o chunk do ficheiro
        String absolutPath = System.getProperty("user.dir");
        String path = filePath(filename,absolutPath);
        byte[] chunkBytes = readFromFile(path,(int)offset,(int)chunk);
        String fileData = new String(chunkBytes);
        System.out.println("Conteudo: " + fileData);

        // Enviar os dados -> offset, tamanho do chunk, chunk e nome do ficheiro
        FSChunkProtocol.sendChunkPacket(this.s,offsetArr,chunkArr,chunkBytes,filename,this.ip,this.destPort);
    }

    public static void main(String[] args) {
        FFS ffs = new FFS(args[0],args[1]);

        ffs.runServer();
    }
}
