import Network.DataTransfer;
import Network.NetworkConfiguration;
import Network.NetworkPacket;
import Network.PacketType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created as part of the class project for Mobile Computing
 */
public class ServerApplication {
    public static String filePath = System.getProperty("user.dir");

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(NetworkConfiguration.getProperty("port")));

            System.out.println("Server Started...");
            Socket socket = serverSocket.accept();

            System.out.println("Connection Established: " + socket);

            DataTransfer fileTransfer = new DataTransfer(socket);


            int packetSize = 0;
            byte[] fileByteArray = null;
            String fileName = null;
            File file = null;
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            RandomAccessFile raFile = null;
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
                }


                if (packet.getType() == PacketType.CLOSE_INDICATOR || packet.getId() >= file.length()) {
                    NetworkPacket endOfFile = new NetworkPacket(file.length(), PacketType.CLOSE_INDICATOR, 0, null);
                    fileTransfer.sendData(endOfFile);
                    break;
                }
                long offset = packet.getId() - 1;
                raFile.seek(offset);
                bis.read(fileByteArray, 0, packetSize);
                System.out.println(String.format("Sending packet sequence: %d", packet.getId()));
                NetworkPacket fileContents = new NetworkPacket(packet.getId(), PacketType.DATA, fileByteArray.length, Arrays.copyOf(fileByteArray, fileByteArray.length));
                fileTransfer.sendData(fileContents);
            }


            System.out.println(String.format("Requested file %s successfully sent", fileName));

            bis.close();
            fileTransfer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
