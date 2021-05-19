package HttpGw;

import java.net.InetAddress;

public class Connection {
    private InetAddress sourceIp;
    private int sourcePort;

    public Connection(InetAddress ip, int port){
        sourceIp = ip;
        sourcePort = port;
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

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }
}
