import Network.DataTransfer;
import Network.NetworkConfiguration;
import Network.NetworkPacket;
import Network.PacketType;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

/**
 * Created as part of the class project for Mobile Computing
 */
public class ClientApplication {

    public static String storePath = System.getProperty("user.dir");

    public static void main(String[] args) {
        try {
            String serverName = NetworkConfiguration.getProperty("host");
            int port = Integer.parseInt(NetworkConfiguration.getProperty("port"));
            Socket helloWorldSocket = new Socket(serverName, port);
            String fileName = NetworkConfiguration.getProperty("file");
            NetworkPacket fileNamePacket = new NetworkPacket(
                    1,
                    PacketType.INITIALIZER,
                    100,
                    fileName.getBytes());

            DataTransfer fileTransfer = new DataTransfer(helloWorldSocket);
            fileTransfer.sendData(fileNamePacket);

            FileOutputStream fout = new FileOutputStream(storePath + fileName);
            RandomAccessFile arrayFile = new RandomAccessFile(storePath + fileName, "rw");
            BufferedOutputStream bos = new BufferedOutputStream(fout);


            while (true) {

                NetworkPacket packet = fileTransfer.receiveData();
                int offset = (int) packet.getId() - 1;
                int packetSize = offset + 100;
                arrayFile.seek(offset);
                if (packet.getType() == PacketType.CLOSE_INDICATOR) break;
                bos.write(packet.getData(), 0, packet.getLength());
                System.out.println("Packet with sequence " + packet.getId() + " received");
                packet = new NetworkPacket(packetSize + 1, PacketType.ACKNOWLEDGEMENT, 0, null);
                fileTransfer.sendData(packet);

            }
            System.out.println("File Downloaded...");

            bos.close();
            fileTransfer.close();
            helloWorldSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
