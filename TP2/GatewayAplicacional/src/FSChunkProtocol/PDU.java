package FSChunkProtocol;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class PDU{
    private int type;
    private InetAddress ip;
    private int port;
    private int transferId;
    private int checksum;
    private int offset;
    private int packetId;
    private byte[] data;

    public PDU() {
        this.type = -1;
        try {
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            e.printStackTrace();
        }
        this.port = -1;
        this.transferId = -1;
        this.checksum = -1;
        this.offset = -1;
        this.packetId = -1;
        this.data = null;
    }

    public PDU(int type){
        this.type = type;
        try {
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            e.printStackTrace();
        }
        this.port = -1;
        this.transferId = -1;
        this.checksum = -1;
        this.offset = -1;
        this.packetId = -1;
        this.data = null;
    }

    public PDU(int type, int packetId){
        this.type = type;
        try {
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            e.printStackTrace();
        }
        this.port = -1;
        this.transferId = -1;
        this.checksum = -1;
        this.offset = -1;
        this.packetId = packetId;
        this.data = null;
    }

    public PDU(int type, int packetId, byte[] data){
        this.type = type;
        try {
            this.ip = InetAddress.getLocalHost();
        } catch(Exception e){
            e.printStackTrace();
        }
        this.port = -1;
        this.transferId = -1;
        this.checksum = -1;
        this.offset = -1;
        this.packetId = packetId;
        this.data = data;
    }

    public PDU(int type, InetAddress ip, int port, int transferId){
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.transferId = transferId;
        this.checksum = -1;
        this.offset = -1;
        this.packetId = -1;
        this.data = null;
    }

    public PDU(int type, InetAddress ip, int port, int transferId, int checksum, int offset, int packetId, byte[] data) {
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.transferId = transferId;
        this.checksum = checksum;
        this.offset = offset;
        this.packetId = packetId;
        this.data = data;
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

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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
        sb.append(", packetId=").append(packetId);
        sb.append(", data=").append(Arrays.toString(data));
        sb.append('}');
        return sb.toString();
    }
}
