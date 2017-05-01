package network;

import common.Tuple;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;

/**
 * Created as part of the class project for Mobile Computing
 */
public class SocketEndPoint implements Callable<Tuple<NetworkPacket, Double>> {

    private NetworkPacket networkPacket;
    private Socket socket;
    private DataTransfer dataTransfer;
    private String endPointName;
    private ConnectionProperties connectionProperties;

    public SocketEndPoint(String name) throws IOException {
        setEndPointName(name);
        String serverName = NetworkConfiguration.getProperty("host", "localhost");
        int port = Integer.parseInt(NetworkConfiguration.getProperty("port", String.valueOf(12500)));
        this.socket = new Socket(serverName, port);
        this.dataTransfer = new DataTransfer(socket);
        this.connectionProperties = new ConnectionProperties();
    }

    public ConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    public String getEndPointName() {
        return endPointName;
    }

    public void setEndPointName(String endPointName) {
        this.endPointName = endPointName;
    }

    public void setTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    @Override
    public Tuple<NetworkPacket, Double> call() throws Exception {
        try {

            NetworkPacket packet;

            dataTransfer.sendData(getNetworkPacket());
            long startTime = System.currentTimeMillis(), endTime;
            do {
                packet = dataTransfer.receiveData();
                if (packet == null)
                    break;
            }
            while (packet.getId() < getNetworkPacket().getId() || (getNetworkPacket().getType() == PacketType.PING && packet.getType() != PacketType.PING));

            endTime = System.currentTimeMillis();

            return new Tuple<>(packet, (double) (endTime - startTime));

        } catch (Exception ex) {
            close();
            return null;
        }
    }


    public void close() throws IOException {
        dataTransfer.close();
        socket.close();
        System.out.println("End Points closed..");
    }

    public NetworkPacket getNetworkPacket() {
        return networkPacket;
    }

    public void setNetworkPacket(NetworkPacket networkPacket) {
        this.networkPacket = networkPacket;
    }

    public class ConnectionProperties {
        private double est_rtt;
        private double dev_rtt;

        public double getEst_rtt() {
            return est_rtt;
        }

        public void setEst_rtt(double est_rtt) {
            this.est_rtt = est_rtt;
        }

        public double getDev_rtt() {
            return dev_rtt;
        }

        public void setDev_rtt(double dev_rtt) {
            this.dev_rtt = dev_rtt;
        }
    }
}

