import Network.DataTransfer;
import Network.NetworkConfiguration;
import Network.NetworkPacket;
import Network.PacketType;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Created as part of the class project for Mobile Computing
 */
public class SocketEndPoint implements Callable<NetworkPacket>, Runnable {
    private Object schedulerCall;
    private Scheduler scheduler;
    private String storePath;
    private String name;
    private NetworkPacket networkPacket;
    private Socket socket;
    private DataTransfer dataTransfer;

    public SocketEndPoint(Object schedulerCall, Scheduler scheduler, String storePath, String name) {
        this.schedulerCall = schedulerCall;
        this.scheduler = scheduler;
        this.storePath = storePath;
        this.name = name;
    }

    public SocketEndPoint() throws IOException {
        String serverName = NetworkConfiguration.getProperty("host");
        int port = Integer.parseInt(NetworkConfiguration.getProperty("port"));
        this.socket = new Socket(serverName, port);
        this.dataTransfer = new DataTransfer(socket);
    }

    @Deprecated
    @Override
    public void run() {
        try {
            String serverName = NetworkConfiguration.getProperty("host");

            System.out.println("Connecting to " + serverName);
            int port = Integer.parseInt(NetworkConfiguration.getProperty("port"));
            Socket clientSocket = new Socket(serverName, port);
            String fileName = NetworkConfiguration.getProperty("file");
            int perPacketSize = 100;
            NetworkPacket fileNamePacket = new NetworkPacket(
                    1,
                    PacketType.INITIALIZER,
                    perPacketSize,
                    fileName.getBytes());

            DataTransfer fileTransfer = new DataTransfer(clientSocket);
            fileTransfer.sendData(fileNamePacket);

            FileOutputStream fout = new FileOutputStream(storePath + fileName);
            RandomAccessFile arrayFile = new RandomAccessFile(storePath + fileName, "rw");
            BufferedOutputStream bos = new BufferedOutputStream(fout);

            NetworkPacket packet = fileTransfer.receiveData();
            if (packet.getType() == PacketType.ACKNOWLEDGEMENT) {

                System.out.println("Sent Initial packet");
            }


            while (true) {
                synchronized (scheduler) {
                    while (!scheduler.getOwner().equals(name)) {
                        try {
                            System.out.println("I'm not the Owner. Thread " + name + " is sleeping...");
//                            scheduler.notify();
                            scheduler.wait();
                            System.out.println(name + " Woke UP!!");

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                packet = new NetworkPacket(scheduler.getOffset(), PacketType.ACKNOWLEDGEMENT, 0, null);
                System.out.println(String.format("Thread %s is sending a packet", name));
                fileTransfer.sendData(packet);
                packet = fileTransfer.receiveData();
                int offset = (int) packet.getId() - 1;
                int packetSize = offset + perPacketSize;
                arrayFile.seek(offset);

                if (packet.getType() == PacketType.CLOSE_INDICATOR)
                    break;

                bos.write(packet.getData(), 0, packet.getLength());
                System.out.println("Packet with sequence " + packet.getId() + " received");

                synchronized (scheduler) {
                    scheduler.setOffset(packetSize + 1);
                    scheduler.setOwner("SCH");
                    scheduler.notifyAll();
                }
//                packet = new NetworkPacket(packetSize + 1, PacketType.ACKNOWLEDGEMENT, 0, null);
//                fileTransfer.sendData(packet);

            }
            System.out.println("File Downloaded...");


            synchronized (scheduler) {
                scheduler.setTransferFinished(true);
                scheduler.setOwner("SCH");
                scheduler.notify();
            }


            bos.close();
            fileTransfer.close();
            clientSocket.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public NetworkPacket call() throws Exception {
        dataTransfer.sendData(getNetworkPacket());
        return dataTransfer.receiveData();
    }

    public NetworkPacket getNetworkPacket() {
        return networkPacket;
    }

    public void setNetworkPacket(NetworkPacket networkPacket) {
        this.networkPacket = networkPacket;
    }
}

