package FSChunkProtocol;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PDU implements Packet{
    private int type;
    private InetAddress ip;
    private int port;
    private int transferId;
    private byte[] data;

    public PDU(){
        this.type = -1;
	    try{
	        this.ip = InetAddress.getLocalHost();
	    } catch(Exception e){
	        System.out.println("Couldn't get local host ip, changing to 10.1.1.1");
	        try {
                this.ip = InetAddress.getByName("10.1.1.1");
            } catch(Exception e2){
	            e2.printStackTrace();
            }
	    }
        this.port = -1;
        this.transferId = -1;
        this.data = null;
    }

    public PDU(int type){
        this.type = type;
        try{
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            System.out.println("Couldn't get local host ip, changing to 10.1.1.1");
            try {
                this.ip = InetAddress.getByName("10.1.1.1");
            } catch(Exception e2){
                e2.printStackTrace();
            }
        }
        this.port = -1;
        this.transferId = -1;
        this.data = null;
    }

    public PDU(int type, byte[] data){
        this.type = type;
        try{
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            System.out.println("Couldn't get local host ip, changing to 10.1.1.1");
            try {
                this.ip = InetAddress.getByName("10.1.1.1");
            } catch(Exception e2){
                e2.printStackTrace();
            }
        }
        this.port = -1;
        this.transferId = -1;
        this.data = data;
    }

    public PDU(int type, byte[] data, int transferId){
        this.type = type;
        try{
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            System.out.println("Couldn't get local host ip, changing to 10.1.1.1");
            try {
                this.ip = InetAddress.getByName("10.1.1.1");
            } catch(Exception e2){
                e2.printStackTrace();
            }
        }
        this.port = -1;
        this.transferId = transferId;
        this.data = data;
    }

    public PDU(int type, InetAddress ip, int port, int transferId){
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.transferId = transferId;
        this.data = null;
    }

    public PDU(int type, InetAddress ip, int port, int transferId, byte[] data) {
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.transferId = transferId;
        this.data = data;
    }

    public static byte[] conversion(int x){
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(x);
        return bb.array();
    }

    public static byte[] conversionFromLong(long x){
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
        bb.putLong(x);
        return bb.array();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public InetAddress getIp(){ return ip;}

    public void setIp(InetAddress ip) {this.ip = ip;}

    public int getPort(){ return port;}

    public void setPort(int port) {this.port = port;}

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setData(long data){
        this.data = conversionFromLong(data);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PDU{");
        sb.append("type=").append(type);
        sb.append(", ip=").append(ip);
        sb.append(", port=").append(port);
        sb.append(", transferId=").append(transferId);
        sb.append(", data=").append(Arrays.toString(data));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public byte[] serialize(){
        byte[] data = this.getData();
        byte[] content;
        if(data != null)
            content = new byte[4 * 2 + data.length];
        else
            content = new byte[4 * 2];
        byte[] type = conversion(this.getType());
        byte[] transferId = conversion(this.getTransferId());

        System.arraycopy(type,0,content,0,4);
        System.arraycopy(transferId,0,content,4,4);
        if(data != null)
            System.arraycopy(data,0,content,8,data.length);

        return content;
    }

    @Override
    public void desserialize(DatagramPacket p) {
        byte[] content = p.getData();
        int size = content.length;
        this.type = ByteBuffer.wrap(content, 0, 4).getInt();
        this.ip = p.getAddress();
        this.port = p.getPort();
        this.transferId = ByteBuffer.wrap(content, 4, 4).getInt();

        byte[] data = null;
        if(size > (4 * 2)) {
            int restSize = size - (4*2);
            data = new byte[restSize];
            System.arraycopy(content,4*2,data,0,restSize);
        }
        this.data = data;
    }
}
