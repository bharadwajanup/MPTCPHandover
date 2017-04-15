import network.NetworkPacket;

import java.io.*;
import java.util.concurrent.BlockingQueue;

/**
 * Created as part of the class project for Mobile Computing
 */
public class PacketDownloader implements Runnable {

    private String path;
    private BlockingQueue<NetworkPacket> packets;

    public PacketDownloader(BlockingQueue<NetworkPacket> queue, String path) {
        this.packets = queue;
        this.path = path;
        System.out.println("Downloading to path " + path);

    }

    @Override
    public void run() {
        FileOutputStream fout = null;
        RandomAccessFile arrayFile = null;
        BufferedOutputStream bos = null;
        try {
            fout = new FileOutputStream(path);
            arrayFile = new RandomAccessFile(path, "rw");
            bos = new BufferedOutputStream(fout);
            while (true) {
                NetworkPacket packet = packets.take();
                arrayFile.seek(packet.getId() - 1);
                bos.write(packet.getData(), 0, packet.getLength());
//                System.out.println(new String(packet.getData()));
                System.out.println(String.format("Worker FileDownloader stored the packet %d", packet.getId()));
            }
        } catch (FileNotFoundException fex) {
            System.out.println("File Not Found");
            fex.printStackTrace();

        } catch (InterruptedException e) {
            System.out.println("Worker Thread was Interrupted");
            try {
                bos.close();
                arrayFile.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //e.printStackTrace();
        } catch (IOException e) {
            System.out.println("There was an IO exception");
            e.printStackTrace();
        }
    }
}
