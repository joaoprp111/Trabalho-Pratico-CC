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
    private DatagramSocket s; // Socket UDP para comunicar com o gateway
    private InetAddress ip;
    private int destPort;
    private final String targetFilesDir = "/files/"; // Diretoria onde guardamos os ficheiros, pode variar conforme o sítio onde se corre o programa

    public FFS(String gatewayIp, String gatewayPort){
        try{
            s = new DatagramSocket();
            ip = InetAddress.getByName(gatewayIp);
            destPort = Integer.parseInt(gatewayPort);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Remover o 'lixo' do array de bytes
     * @param source array de bytes
     * @return       array de bytes sem o 'lixo'
     */
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

    /**
     * Calculo do caminho do ficheiro
     * @param cleanFilename array com o nome do ficheiro
     * @param absolutPath   caminho onde o programa está a correr (diretoria atual)
     * @return              Caminho total do ficheiro
     */
    private String filePath(byte[] cleanFilename, String absolutPath){
        String cleanFilenameStr = new String(cleanFilename);
        StringBuilder sb = new StringBuilder(absolutPath);
        sb.append(this.targetFilesDir).append(cleanFilenameStr);
        return sb.toString();
    }

    /**
     * Ativação do servidor, tem duas threads, uma para enviar beacons e outra para responder a pedidos
     */
    public void runServer(){

        //Criar uma thread para as beacons
        new Thread(() -> {
            FSChunkProtocol.sendBeacons(s, ip, destPort);
        }).start();

        //Criar outra para tratar os pedidos vindos do gateway
        new Thread(() -> {
            while(true) {
                PDU p = FSChunkProtocol.receivePacket(s);
                int type = p.getType();
                switch(type){
                    case 2:
                        // Pedido de metadados
                        response(p);
                        break;
                    case 4:
                        // Pedido de chunk
                        sendChunk(p);
                        break;
                }
            }
        }).start();
    }

    /**
     * Resposta a pedidos do tipo 2, envio dos metadados
     * @param p     Pacote do tipo 2
     */
    public void response(PDU p){
        byte[] filename = removeTrash(p.getData());
        String absolutPath = System.getProperty("user.dir");
        String path = filePath(filename,absolutPath);
        File file = new File(path);
        try{
            Scanner sc = new Scanner(file);
            FSChunkProtocol.sendResponse(this.s,file,filename,this.ip,this.destPort);
        } catch(FileNotFoundException e){
            System.out.println("O ficheiro não existe!");
        }
    }

    /**
     * Ler um chunk do ficheiro
     * @param path  Caminho do ficheiro
     * @param offset Offset a partir do qual o chunk deve ser lido
     * @param size  Tamanho do chunk a ser lido
     * @return      Array de bytes com o chunk
     */
    public byte[] readFromFile(String path,int offset, int size){
        byte[] content = new byte[size];
        try {
            RandomAccessFile f = new RandomAccessFile(path,"r");
            f.seek(offset);
            f.read(content,0,size);
        } catch(Exception e){
            e.printStackTrace();
        }
        return content;
    }

    /**
     * Resposta a pedidos do tipo 4
     * @param p Pacote do tipo 4
     */
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

        // Ler o chunk do ficheiro
        String absolutPath = System.getProperty("user.dir");
        String path = filePath(filename,absolutPath);
        byte[] chunkBytes = readFromFile(path,(int)offset,(int)chunk);

        // Enviar os dados -> offset, tamanho do chunk, chunk e nome do ficheiro
        FSChunkProtocol.sendChunkPacket(this.s,offsetArr,chunkArr,chunkBytes,filename,this.ip,this.destPort);
    }

    /**
     * Main do servidor
     * @param args Ip e porta da entidade a quem vai prestar serviço
     */
    public static void main(String[] args) {
        FFS ffs = new FFS(args[0],args[1]);

        ffs.runServer();
    }
}
