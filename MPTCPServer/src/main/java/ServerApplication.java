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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created as part of the class project for Mobile Computing
 */
public class ServerApplication implements Runnable {
    public static String filePath;
    Socket sock;
    NetworkRTTInterpolator interpolator = null;
    private RandomAccessFile raFile;
    private int packetSize;

    public ServerApplication(Socket sock) {
        this.sock = sock;
    }

    public synchronized static void print(String message) {

        System.out.println(message);
    }

    public static void main(String[] args) throws InterruptedException, IOException {


        ServerSocket serverSocket = null;
        filePath = NetworkConfiguration.getProperty("server_directory", System.getProperty("user.dir")) + System.getProperty("file.separator") + NetworkConfiguration.getProperty("server_directory_name", "data");

        try {
            serverSocket = new ServerSocket(Integer.parseInt(NetworkConfiguration.getProperty("port", String.valueOf(12500))));
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Server Started...");
        while (true) {
            try {
                System.out.println("Accepting Connections");
                Socket socket = serverSocket.accept();
                System.out.println("Connection Established " + socket);
                ExecutorService executor = Executors.newFixedThreadPool(4);
                executor.execute(new ServerApplication(socket));
            } catch (IOException ex) {
                ex.printStackTrace();
                //newThread.join();
                break;
            } catch (Exception e) {
                System.out.println("I was interrupted...");
                e.printStackTrace();
                break;
            }

        }


    }

    @Override
    public void run() {
        DataTransfer fileTransfer = null;
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ArrayBlockingQueue<NetworkPacket> queue = new ArrayBlockingQueue<NetworkPacket>(1024);
        Lock lock = new ReentrantLock();

        Condition clearCondition = lock.newCondition();

        try {
            fileTransfer = new DataTransfer(sock);
            PacketSender sender = new PacketSender(queue, fileTransfer, lock, clearCondition);
            executor.execute(sender);

            while (true) {
                NetworkPacket packet = fileTransfer.receiveData();

                lock.lock();
                clearCondition.signal();
                lock.unlock();

                String endPointName = packet.getEndPoint();

                switch (packet.getType()) {
                    case INITIALIZER:
                        packet = initializeTransfer(packet);
                        break;
                    case PING:
                        if (packet.getId() >= raFile.length())
                            packet = createDummyPacketOfType(packet.getId(), PacketType.CLOSE_INDICATOR);
                        else
                            packet = createDummyPacketOfType(packet.getId(), PacketType.PING);
                        break;
                    case CLOSE_INDICATOR:
                        packet = createDummyPacketOfType(packet.getId(), PacketType.CLOSE_INDICATOR);
                        break;
                    default:
                        if (packet.getId() >= raFile.length())
                            packet = createDummyPacketOfType(packet.getId(), PacketType.CLOSE_INDICATOR);
                        else
                            packet = createDataPacket(packet.getId());

                }

                int sleepVal = (int) interpolator.getY(packet.getId(), endPointName);
                ServerApplication.print(String.format("%d %d %s %s", sleepVal, packet.getId(), endPointName, packet.getType().name()));


                packet.setEndPoint(endPointName);
                packet.setLatency(sleepVal);

                queue.add(packet);

                if (packet.getType() == PacketType.CLOSE_INDICATOR || packet.getId() >= raFile.length()) {
                    while (queue.size() > 0) ;
                    Thread.sleep(1000);
                    break;
                }
            }

            System.out.println(String.format("Requested file %s successfully sent", ""));

        } catch (EOFException ex) {
            System.out.println("EOF EXCEPTION");
            System.out.println(Thread.currentThread().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("Threads interrupted!");
            try {
                if (fileTransfer != null) {
                    fileTransfer.close();
                }
                raFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private synchronized NetworkPacket createDataPacket(long id) throws IOException {
        byte[] fileByteArray = new byte[packetSize];
        int readLength;
        raFile.seek(id - 1);
        readLength = raFile.read(fileByteArray);
        return new NetworkPacket(id, PacketType.DATA, readLength, Arrays.copyOf(fileByteArray, readLength), null);
    }

    private NetworkPacket createDummyPacketOfType(long id, PacketType packetType) {
        return new NetworkPacket(id, packetType, 0, null, null);
    }

    private synchronized NetworkPacket initializeTransfer(NetworkPacket packet) throws IOException {


        String fileName = new String(packet.getData());

        String path = filePath + "/" + fileName;
        File file = new File(path);
        System.out.println(path);
        raFile = new RandomAccessFile(file, "r");


        if(packet.getEndPoint().equals("Wi-Fi")) {
            double[] rand = {155.0, 165, 175, 185, 195, 205, 215, 225, 255, 285, 325, 375, 485, 675, 1055, 1580, 3500, 4680, 2472, 1300, 850, 565, 345, 335, 325, 315, 295, 275, 255, 235, 215, 195, 155, 150, 155, 150, 155};
            interpolator = new NetworkRTTInterpolator(raFile.length() + 1000, rand);
        } else {
            double[] rand = {80, 82, 81, 86, 92, 83, 81, 84, 82, 80, 78, 81, 86, 85};
            interpolator = new NetworkRTTInterpolator(raFile.length() + 1000, rand);
        }


        packetSize = packet.getLength();
        NetworkPacket ackPacket = new NetworkPacket(1, PacketType.ACKNOWLEDGEMENT, 0, null, null);
        System.out.println("Sending ACK for Initializer");
        return ackPacket;
    }
}
