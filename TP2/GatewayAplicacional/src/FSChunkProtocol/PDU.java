package FSChunkProtocol;

public class PDU {
    private int type;
    private int transferId;
    private int checksum;
    private int offset;
    private int packetId;
    private byte[] data;

    public PDU(int type, int transferId, int checksum, int offset, int packetId, byte[] data) {
        this.type = type;
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
}
