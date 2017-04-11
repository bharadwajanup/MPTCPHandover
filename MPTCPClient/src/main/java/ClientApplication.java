import Network.DataTransfer;
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
    public static String storePath = "/home/rkanchib/Downloads/MC/MPTCPHandover/MPTCPClient/src/main/resources/";

    public static void main(String[] args) {
        try {
            String serverName = "127.0.0.1";
            int port = 10500;
            Socket helloWorldSocket = new Socket(serverName, port);
            String fileName = "proposal.pptx";
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
