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
    private int checksum;
    private int offset;
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
        this.checksum = -1;
        this.offset = -1;
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
        this.checksum = -1;
        this.offset = -1;
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
        this.checksum = -1;
        this.offset = -1;
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
        this.checksum = -1;
        this.offset = -1;
        this.data = data;
    }

    public PDU(int type, InetAddress ip, int port, int transferId){
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.transferId = transferId;
        this.checksum = -1;
        this.offset = -1;
        this.data = null;
    }

    public PDU(int type, InetAddress ip, int port, int transferId, int checksum, int offset, byte[] data) {
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.transferId = transferId;
        this.checksum = checksum;
        this.offset = offset;
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

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
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
        sb.append(", checksum=").append(checksum);
        sb.append(", offset=").append(offset);
        sb.append(", data=").append(Arrays.toString(data));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public byte[] serialize(){
        byte[] data = this.getData();
        byte[] content;
        if(data != null)
            content = new byte[4 * 4 + data.length];
        else
            content = new byte[4 * 4];
        byte[] type = conversion(this.getType());
        byte[] transferId = conversion(this.getTransferId());
        byte[] checksum = conversion(this.getChecksum());
        byte[] offset = conversion(this.getOffset());

        System.arraycopy(type,0,content,0,4);
        System.arraycopy(transferId,0,content,4,4);
        System.arraycopy(checksum,0,content,8,4);
        System.arraycopy(offset,0,content,12,4);
        if(data != null)
            System.arraycopy(data,0,content,16,data.length);

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
        this.checksum = ByteBuffer.wrap(content, 8, 4).getInt();
        this.offset = ByteBuffer.wrap(content, 12, 4).getInt();

        byte[] data = null;
        if(size > (4 * 4)) {
            int restSize = size - (4*4);
            data = new byte[restSize];
            System.arraycopy(content,4*4,data,0,restSize);
        }
        this.data = data;
    }
}
