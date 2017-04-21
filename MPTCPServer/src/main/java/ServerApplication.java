import network.DataTransfer;
import network.NetworkConfiguration;
import network.NetworkPacket;
import network.PacketType;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

/**
 * Created as part of the class project for Mobile Computing
 */
public class ServerApplication implements Runnable {
    public static String filePath = System.getProperty("user.dir");
    Socket sock;
    NetworkRTTInterpolator interpolator = null;
    private RandomAccessFile raFile;
    private int packetSize;

    public ServerApplication(Socket sock) {
        this.sock = sock;
    }

    public static void main(String[] args) throws InterruptedException {


        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Integer.parseInt(NetworkConfiguration.getProperty("port")));
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Server Started...");
        Thread newThread = null;
        int counter = 1;
        while (true) {
            try {
                System.out.println("Accepting Connections");
                Socket socket = serverSocket.accept();
                System.out.println("Connection Established " + socket);

                newThread = new Thread(new ServerApplication(socket));
                String name = "Thread-" + counter;

                newThread.setName(name);
                counter++;
                System.out.println("Thread " + name + " created.");
                newThread.start();

                if (newThread.isAlive())
                    System.out.println("Thread is still alive!");
                else
                    System.out.println("Thread is dead!");

            } catch (IOException ex) {
                ex.printStackTrace();
                newThread.join();
                break;
            }
        }


    }

    @Override
    public void run() {
        try {
            DataTransfer fileTransfer = new DataTransfer(sock);
            Random random = new Random();
            while (true) {
                NetworkPacket packet = fileTransfer.receiveData();

                switch (packet.getType()) {
                    case INITIALIZER:
                        packet = initializeTransfer(packet);
                        break;
                    case PING:
                        packet = createEmptyPacketOfType(packet.getId(), PacketType.PING);
                        break;
                    case CLOSE_INDICATOR:
                        packet = createEmptyPacketOfType(packet.getId(), PacketType.CLOSE_INDICATOR);
                        break;
                    default:
                        if (packet.getId() >= raFile.length())
                            packet = createEmptyPacketOfType(packet.getId(), PacketType.CLOSE_INDICATOR);
                        else
                            packet = createDataPacket(packet.getId());


                }
                int sleepVal = (int) interpolator.getY(packet.getId());
                System.out.println("Sleeping for " + sleepVal);
                Thread.sleep(sleepVal);
                packet.setLatency(sleepVal);
                System.out.println("Sending " + packet.getId());
                fileTransfer.sendData(packet);

                if (packet.getType() == PacketType.CLOSE_INDICATOR || packet.getId() >= raFile.length()) {
                    break;
                }
            }

            System.out.println(String.format("Requested file %s successfully sent", ""));
//            bis.close();
            raFile.close();
            fileTransfer.close();
        } catch (EOFException ex) {
            System.out.println("EOF EXCEPTION");
            System.out.println(Thread.currentThread().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private synchronized NetworkPacket createDataPacket(long id) throws IOException {
        byte[] fileByteArray = new byte[packetSize];
        int readLength;
        raFile.seek(id - 1);
        readLength = raFile.read(fileByteArray);
//        readLength = bis.read(fileByteArray, 0, packetSize);
        return new NetworkPacket(id, PacketType.DATA, readLength, Arrays.copyOf(fileByteArray, readLength));
    }

    private NetworkPacket createEmptyPacketOfType(long id, PacketType packetType) {
        return new NetworkPacket(id, packetType, 0, null);
    }

    private synchronized NetworkPacket initializeTransfer(NetworkPacket packet) throws IOException {
        String fileName = new String(packet.getData());

        String path = filePath + "\\" + "MPTCPServer\\" + fileName;
        File file = new File(path);
        System.out.println(path);
        raFile = new RandomAccessFile(file, "r");
        interpolator = new NetworkRTTInterpolator(raFile.length() + 1000);

//        FileInputStream fis = new FileInputStream(file);
//        bis = new BufferedInputStream(fis);
        packetSize = packet.getLength();
//        fileByteArray = new byte[packetSize];
        NetworkPacket ackPacket = new NetworkPacket(1, PacketType.ACKNOWLEDGEMENT, 0, null);
        System.out.println("Sending ACK for Initiliazer");
        return ackPacket;
    }
}
