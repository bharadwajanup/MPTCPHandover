import network.DataTransfer;
import network.NetworkConfiguration;
import network.NetworkPacket;
import network.PacketType;

import java.io.*;
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

            int packetSize = 0;
            byte[] fileByteArray = null;
            String fileName = null;
            File file = null;
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            RandomAccessFile raFile = null;
            int readLength;
            while (true) {
                NetworkPacket packet = fileTransfer.receiveData();

                if (packet.getType() == PacketType.INITIALIZER) {
                    fileName = new String(packet.getData());


                    file = new File(filePath + "\\" + fileName);
                    raFile = new RandomAccessFile(file, "r");

                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    packetSize = packet.getLength();
                    fileByteArray = new byte[packetSize];
                    NetworkPacket ackPacket = new NetworkPacket(1, PacketType.ACKNOWLEDGEMENT, 0, null);
                    System.out.println("Sending ACK for Initiliazer");
                    fileTransfer.sendData(ackPacket);
                    continue;
                }


                if (packet.getType() == PacketType.CLOSE_INDICATOR || packet.getId() >= file.length()) {
                    NetworkPacket endOfFile = new NetworkPacket(file.length(), PacketType.CLOSE_INDICATOR, 0, null);
                    fileTransfer.sendData(endOfFile);
                    break;
                }
                long offset = packet.getId() - 1;
                raFile.seek(offset);
                readLength = bis.read(fileByteArray, 0, packetSize);
//                System.out.println(String.format("Sending packet sequence: %d", packet.getId()));
                NetworkPacket fileContents = new NetworkPacket(packet.getId(), PacketType.DATA, readLength, Arrays.copyOf(fileByteArray, readLength));
                Thread.sleep(random.nextInt(1000));
                fileTransfer.sendData(fileContents);
            }


            System.out.println(String.format("Requested file %s successfully sent", fileName));

            bis.close();
            fileTransfer.close();
        } catch (EOFException ex) {
            System.out.println("EOF EXCEPTION");
            System.out.println(Thread.currentThread().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
