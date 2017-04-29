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

    public static Tuple<NetworkPacket, Double> calculateLatency(SocketEndPoint endPoint, ExecutorService executor, Scheduler scheduler) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis(), endTime;
        Future<NetworkPacket> future = executor.submit(endPoint);
        NetworkPacket packet = null;

        try {
            int timeout = 0;
            if (!endPoint.getEndPointName().equals("LTE"))
                timeout = (int) scheduler.getTimeout();
            endPoint.setTimeout(timeout);
            packet = future.get();
            endTime = System.currentTimeMillis();
            return new Tuple<>(packet, (double) (endTime - startTime));
        } catch (Exception t) {
            endTime = System.currentTimeMillis();
            return new Tuple<>(packet, (double) (endTime - startTime));
        }

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
        long startTime, endTime;
        int perPacketSize = 100;
        String fileName = NetworkConfiguration.getProperty("file");
        long curAck;

        NetworkPacket packet = new NetworkPacket(
                1,
                PacketType.INITIALIZER,
                perPacketSize,
                fileName.getBytes(),
                null);


        Scheduler scheduler = new Scheduler(10);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        SocketEndPoint wifiPacket = new SocketEndPoint("Wi-Fi");
        SocketEndPoint ltePacket = new SocketEndPoint("LTE");
        SocketEndPoint sendingEndPoint;

        ArrayBlockingQueue<NetworkPacket> blockQueue = new ArrayBlockingQueue<>(1024);
        PacketDownloader downloadPacket = new PacketDownloader(blockQueue, storePath + "/" + fileName);
        executor.execute(downloadPacket);

        packet.setEndPoint(wifiPacket.getEndPointName());
        wifiPacket.setNetworkPacket(packet);
        Tuple<NetworkPacket, Double> result = calculateLatency(wifiPacket, executor, scheduler);
        scheduler.update(result.y);

        packet.setEndPoint(ltePacket.getEndPointName());
        ltePacket.setNetworkPacket(packet);
        result = calculateLatency(ltePacket, executor, scheduler);
        scheduler.update(result.y);

        curAck = packet.getId();

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        int i = 0, j = 0;
        while (true) {


            if (scheduler.isMainFlow()) {
                sendingEndPoint = wifiPacket;
//                System.out.println("Scheduled to send via " + sendingEndPoint.getEndPointName());
//                System.out.println(curAck);
                packet = new NetworkPacket(curAck, PacketType.ACKNOWLEDGEMENT, 0, null, sendingEndPoint.getEndPointName());
                sendingEndPoint.setNetworkPacket(packet);
                result = calculateLatency(sendingEndPoint, executor, scheduler);
                scheduler.update(result.y);
//                printRTTLog(sendingEndPoint, result.y);
                packet = result.x;
                if (packet != null && packet.getId() < curAck) {
                    i++;
                }
                if (packet != null)
                    endTime = System.currentTimeMillis();

            } else {
                sendingEndPoint = ltePacket;
//                System.out.println(curAck);
//                System.out.println("Scheduled to send via " + sendingEndPoint.getEndPointName());
                packet = new NetworkPacket(curAck, PacketType.ACKNOWLEDGEMENT, 0, null, sendingEndPoint.getEndPointName());
                sendingEndPoint.setNetworkPacket(packet);
                result = calculateLatency(sendingEndPoint, executor, scheduler);
                packet = result.x;

                if (packet != null)
                    endTime = System.currentTimeMillis();

//                printRTTLog(sendingEndPoint, result.y);
                wifiPacket.setNetworkPacket(new NetworkPacket(curAck, PacketType.PING, 0, null, wifiPacket.getEndPointName()));
                result = calculateLatency(wifiPacket, executor, scheduler);
//                scheduler.updateTable(sendingEndPoint, result.y);
//                if(result.x == null)
//                    System.out.println("Ping Timeout: "+curAck);
//                System.out.println("Ping: "+result.y);
                scheduler.update(result.y);
//                printRTTLog(wifiPacket, result.y);
//                System.out.println("RTT= " + result.y);
                if (packet != null && packet.getId() < curAck) {
                    j++;
                }

            }
            if (packet == null) {
                scheduler.setMainFlow(false);
//                System.out.println("Timeout: "+curAck);
                continue;
            }
            if (packet.getType() == PacketType.CLOSE_INDICATOR)
                break;

            if (packet.getId() >= curAck && packet.getType() == PacketType.DATA) {
                System.out.println(String.format("%d %d", packet.getId(), (endTime - startTime)));
                curAck = packet.getId() + perPacketSize;
                blockQueue.add(packet);
                startTime = System.currentTimeMillis();
            }
        }
        System.out.println("Wifi: " + i);
        System.out.println("Lte: " + j);
        System.out.println("File Downloaded...");
        wifiPacket.close();
        ltePacket.close();
        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);
        if (!terminated)
            executor.shutdownNow();
    }

    private static void printRTTLog(SocketEndPoint sendingEndPoint, Double y) {
        String message = String.format("%s\t%f", sendingEndPoint.getEndPointName(), y);
        System.out.println(message);
    }
}
