import common.Tuple;
import network.NetworkConfiguration;
import network.NetworkPacket;
import network.PacketType;
import network.SocketEndPoint;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created as part of the class project for Mobile Computing
 */
public class ClientApplication {

    public static String storePath = System.getProperty("user.dir");

    public static Tuple<NetworkPacket, Double> calculateLatency(SocketEndPoint endPoint, ExecutorService executor) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        Future<NetworkPacket> future = executor.submit((Callable) endPoint);
        NetworkPacket packet = future.get();
        long endTime = System.currentTimeMillis();
        return new Tuple<>(packet, calcRTT(endPoint, endTime - startTime));
    }

    public static double calcRTT(SocketEndPoint endPoint, long sample_rtt) {
        double est_rtt = endPoint.getConnectionProperties().getEst_rtt();
        double dev_rtt = endPoint.getConnectionProperties().getDev_rtt();

        est_rtt = (0.875 * est_rtt) + (0.125 * sample_rtt);
        dev_rtt = (0.75 * dev_rtt) + (0.25 * (sample_rtt - est_rtt));

        double rtt = est_rtt + 4 * dev_rtt;

        endPoint.getConnectionProperties().setDev_rtt(dev_rtt);
        endPoint.getConnectionProperties().setEst_rtt(est_rtt);

        return rtt;

    }


    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        long startTime, endTime, totalTime;
        int perPacketSize = 100;
        String fileName = NetworkConfiguration.getProperty("file");
        long curAck;

        NetworkPacket packet = new NetworkPacket(
                1,
                PacketType.INITIALIZER,
                perPacketSize,
                fileName.getBytes());


        Scheduler scheduler = new Scheduler();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        SocketEndPoint wifiPacket = new SocketEndPoint("Wi-Fi");
        SocketEndPoint ltePacket = new SocketEndPoint("LTE");
        SocketEndPoint sendingEndPoint;
        ArrayBlockingQueue<NetworkPacket> blockQueue = new ArrayBlockingQueue<NetworkPacket>(1024);
        PacketDownloader downloadPacket = new PacketDownloader(blockQueue, storePath + "/" + fileName);
        executor.execute(downloadPacket);

        wifiPacket.setNetworkPacket(packet);
        Tuple<NetworkPacket, Double> result = calculateLatency(wifiPacket, executor);
        scheduler.addToTable(wifiPacket, result.y);

        ltePacket.setNetworkPacket(packet);
        result = calculateLatency(ltePacket, executor);
        scheduler.addToTable(ltePacket, result.y);

        curAck = packet.getId();

        while (true) {
            sendingEndPoint = scheduler.getScheduledEndPoint();
            System.out.println("Scheduled to send via " + sendingEndPoint.getEndPointName());
            packet = new NetworkPacket(curAck, PacketType.ACKNOWLEDGEMENT, 0, null);
            sendingEndPoint.setNetworkPacket(packet);
            result = calculateLatency(sendingEndPoint, executor);
            scheduler.updateTable(sendingEndPoint, result.y);
            System.out.println("RTT= " + result.y);
            packet = result.x;
            if (packet.getType() == PacketType.CLOSE_INDICATOR)
                break;
            curAck = packet.getId() + perPacketSize;
            blockQueue.add(packet);
        }

        System.out.println("File Downloaded...");
        wifiPacket.close();
        ltePacket.close();
        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);
        if (!terminated)
            executor.shutdownNow();
    }
}
