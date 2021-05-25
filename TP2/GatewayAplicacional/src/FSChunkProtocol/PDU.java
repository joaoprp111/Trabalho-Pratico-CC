package FSChunkProtocol;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PDU implements Packet{
    private int type; // Tipo do pacote
    private int transferId; // Id da transferência
    private byte[] data; // Payload (pode ter até 4096 bytes)

    public PDU(){
        this.type = -1;
        this.transferId = -1;
        this.data = null;
    }

    public PDU(int type){
        this.type = type;
        this.transferId = -1;
        this.data = null;
    }

    public PDU(int type, byte[] data){
        this.type = type;
        this.transferId = -1;
        this.data = data;
    }

    public PDU(int type, byte[] data, int transferId){
        this.type = type;
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
        sb.append(", transferId=").append(transferId);
        sb.append(", data=").append(Arrays.toString(data));
        sb.append('}');
        return sb.toString();
    }

    /**
     * Serialização do pacote
     * @return Array com a informação toda do pacote em bytes
     */
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

    /**
     * Deserialização do pacote
     * @param p DatagramPacket com os dados recebidos na transmissão UDP
     */
    @Override
    public void desserialize(DatagramPacket p) {
        byte[] content = p.getData();
        int size = content.length;
        this.type = ByteBuffer.wrap(content, 0, 4).getInt();
        byte[] ip = p.getAddress().getAddress();
        byte[] ipLength = conversion(ip.length);
        byte[] port = conversion(p.getPort());
        this.transferId = ByteBuffer.wrap(content, 4, 4).getInt();

        byte[] data = null;
        if(size > (4 * 2)) {
            int restSize = size - (4*2);
            // Guardar porta, tamanho do ip e ip, mais o resto dos dados nos pedidos beacon
            if(this.type == 1 || this.type == 3) {
                data = new byte[4 + 4 + ip.length + restSize];
                System.arraycopy(port, 0, data, 0, 4);
                System.arraycopy(ipLength, 0, data, 4, 4);
                System.arraycopy(ip, 0, data, 8, ip.length);
                System.arraycopy(content, 4 * 2, data, 8 + ip.length, restSize);
            } else{
                data = new byte[restSize];
                System.arraycopy(content, 4 * 2, data, 0, restSize);
            }
        }
        this.data = data;
    }
}
