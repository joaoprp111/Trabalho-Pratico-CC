package FSChunkProtocol;

import java.net.DatagramPacket;

public interface Packet {
    public void desserialize(DatagramPacket p);
    public byte[] serialize();
}
