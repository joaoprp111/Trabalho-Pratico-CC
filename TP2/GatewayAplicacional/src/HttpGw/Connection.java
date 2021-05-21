package HttpGw;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    private InetAddress sourceIp;
    private int sourcePort;
    private double lastBeaconSeconds;
    private Lock l;

    public Connection(InetAddress ip, int port){
        sourceIp = ip;
        sourcePort = port;
        lastBeaconSeconds = (double) System.nanoTime() / 1000000000;
        l = new ReentrantLock();
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
        return sourcePort == that.sourcePort && Objects.equals(sourceIp, that.sourceIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceIp, sourcePort);
    }
}