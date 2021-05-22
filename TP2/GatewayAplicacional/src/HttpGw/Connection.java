package HttpGw;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    private InetAddress sourceIp;
    private int sourcePort;
    private double lastBeaconSeconds;
    private String currentFileTransfer;
    private long currentFileSize;
    private Lock l;

    public Connection(InetAddress ip, int port){
        sourceIp = ip;
        sourcePort = port;
        lastBeaconSeconds = (double) System.nanoTime() / 1000000000;
        currentFileTransfer = "";
        currentFileSize = -1;
        l = new ReentrantLock();
    }

    public String getCurrentFileTransfer() {
        return currentFileTransfer;
    }

    public void setCurrentFileTransfer(String currentFileTransfer) {
        this.currentFileTransfer = currentFileTransfer;
    }

    public long getCurrentFileSize() {
        return currentFileSize;
    }

    public void setCurrentFileSize(long currentFileSize) {
        this.currentFileSize = currentFileSize;
    }

    public InetAddress getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(InetAddress sourceIp) {
        this.sourceIp = sourceIp;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public double getLastBeaconSeconds(){
        return lastBeaconSeconds;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public void setLastBeaconSeconds(double seconds){
        lastBeaconSeconds = seconds;
    }

    public void lock(){
        l.lock();
    }

    public void unlock(){
        l.unlock();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return sourcePort == that.sourcePort && Double.compare(that.lastBeaconSeconds, lastBeaconSeconds) == 0 && currentFileSize == that.currentFileSize && Objects.equals(sourceIp, that.sourceIp) && Objects.equals(currentFileTransfer, that.currentFileTransfer) && Objects.equals(l, that.l);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceIp, sourcePort, lastBeaconSeconds, currentFileTransfer, currentFileSize, l);
    }
}
