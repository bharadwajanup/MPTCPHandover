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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public synchronized static void print(String message) {

        System.out.println(message);
    }

    public static void main(String[] args) throws InterruptedException, IOException {


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
                ExecutorService executor = Executors.newFixedThreadPool(4);

                //newThread = new ServerApplication(socket);
                /*String name = "Thread-" + counter;

                newThread.setName(name);
                counter++;
                System.out.println("Thread " + name + " created.");

                newThread.start();
                  */

                executor.execute(new ServerApplication(socket));


                /*if (newThread.isAlive())
                    System.out.println("Thread is still alive!");
                else
                    System.out.println("Thread is dead!");
                */
            } catch (IOException ex) {
                ex.printStackTrace();
                //newThread.join();
                break;
            } catch (Exception e) {
                System.out.println("I was interrupted");
            }

        }


    }

    @Override
    public void run() {
        DataTransfer fileTransfer = null;
        try {
            fileTransfer = new DataTransfer(sock);
            while (true) {
                NetworkPacket packet = fileTransfer.receiveData();
                String endPointName = packet.getEndPoint();

                switch (packet.getType()) {
                    case INITIALIZER:
                        packet = initializeTransfer(packet);
                        break;
                    case PING:
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
//                        Thread.sleep(random.nextInt(1000));

                }
                int sleepVal = (int) interpolator.getY(packet.getId(), endPointName);
//                if (packet.getType() != PacketType.PING)
                ServerApplication.print(String.format("%d %d %s %s", sleepVal, packet.getId(), endPointName, packet.getType().name()));
                Thread.sleep(sleepVal);
                packet.setLatency(sleepVal);
//                System.out.println("Sending " + packet.getId() + " of type " + packet.getType().name());
                fileTransfer.sendData(packet);

                if (packet.getType() == PacketType.CLOSE_INDICATOR || packet.getId() >= raFile.length()) {
                    break;
                }
            }

            System.out.println(String.format("Requested file %s successfully sent", ""));
//            bis.close();
//            fileTransfer.close();

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
//        readLength = bis.read(fileByteArray, 0, packetSize);
        return new NetworkPacket(id, PacketType.DATA, readLength, Arrays.copyOf(fileByteArray, readLength), null);
    }

    private NetworkPacket createDummyPacketOfType(long id, PacketType packetType) {
        return new NetworkPacket(id, packetType, 0, null, null);
    }

    private synchronized NetworkPacket initializeTransfer(NetworkPacket packet) throws IOException {
        String fileName = new String(packet.getData());

        String path = filePath + "\\" + "MPTCPServer\\" + fileName;
//        String path = filePath + "\\" + fileName;
        File file = new File(path);
        System.out.println(path);
        raFile = new RandomAccessFile(file, "r");
        interpolator = new NetworkRTTInterpolator(raFile.length() + 1000);

//        FileInputStream fis = new FileInputStream(file);
//        bis = new BufferedInputStream(fis);
        packetSize = packet.getLength();
//        fileByteArray = new byte[packetSize];
        NetworkPacket ackPacket = new NetworkPacket(1, PacketType.ACKNOWLEDGEMENT, 0, null, null);
        System.out.println("Sending ACK for Initiliazer");
        return ackPacket;
    }
}
