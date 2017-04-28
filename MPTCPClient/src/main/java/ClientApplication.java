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

//                packet = future.get();
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
        long startTime, endTime, totalTime;
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
        ArrayBlockingQueue<NetworkPacket> blockQueue = new ArrayBlockingQueue<NetworkPacket>(1024);
        PacketDownloader downloadPacket = new PacketDownloader(blockQueue, storePath + "/" + fileName);
        executor.execute(downloadPacket);
        packet.setEndPoint(wifiPacket.getEndPointName());
        wifiPacket.setNetworkPacket(packet);
        Tuple<NetworkPacket, Double> result = calculateLatency(wifiPacket, executor, scheduler);
//        scheduler.addToTable(wifiPacket, result.y);
        scheduler.update(result.y);
        packet.setEndPoint(ltePacket.getEndPointName());
        ltePacket.setNetworkPacket(packet);
        result = calculateLatency(ltePacket, executor, scheduler);
//        scheduler.addToTable(ltePacket, result.y);
        scheduler.update(result.y);

        curAck = packet.getId();

        while (true) {


            if (scheduler.isMainFlow()) {
                sendingEndPoint = wifiPacket;
//                System.out.println("Scheduled to send via " + sendingEndPoint.getEndPointName());
                packet = new NetworkPacket(curAck, PacketType.ACKNOWLEDGEMENT, 0, null, sendingEndPoint.getEndPointName());
                sendingEndPoint.setNetworkPacket(packet);
                result = calculateLatency(sendingEndPoint, executor, scheduler);
//                scheduler.updateTable(sendingEndPoint, result.y);
                scheduler.update(result.y);
                printRTTLog(sendingEndPoint, result.y);
                packet = result.x;

            } else {
                sendingEndPoint = ltePacket;
//                System.out.println("Scheduled to send via " + sendingEndPoint.getEndPointName());
                packet = new NetworkPacket(curAck, PacketType.ACKNOWLEDGEMENT, 0, null, sendingEndPoint.getEndPointName());
                sendingEndPoint.setNetworkPacket(packet);
                result = calculateLatency(sendingEndPoint, executor, scheduler);
                packet = result.x;

                printRTTLog(sendingEndPoint, result.y);
                wifiPacket.setNetworkPacket(new NetworkPacket(curAck, PacketType.PING, 0, null, wifiPacket.getEndPointName()));
                result = calculateLatency(wifiPacket, executor, scheduler);
//                scheduler.updateTable(sendingEndPoint, result.y);
                scheduler.update(result.y);
                printRTTLog(wifiPacket, result.y);
//                System.out.println("RTT= " + result.y);

            }


//            sendingEndPoint = scheduler.getScheduledEndPoint();
//            System.out.println("Scheduled to send via " + sendingEndPoint.getEndPointName());
//            packet = new NetworkPacket(curAck, PacketType.ACKNOWLEDGEMENT, 0, null);
//            sendingEndPoint.setNetworkPacket(packet);
//            result = calculateLatency(sendingEndPoint, executor);
//            scheduler.updateTable(sendingEndPoint, result.y);
//            System.out.println("RTT= " + result.y);
//            packet = result.x;
            if (packet == null) {
//                scheduler.setMainFlow(!scheduler.isMainFlow());
                continue;
            }
            if (packet.getType() == PacketType.CLOSE_INDICATOR)
                break;

            if (packet.getId() >= curAck && packet.getType() == PacketType.DATA) {
                curAck = packet.getId() + perPacketSize;
                blockQueue.add(packet);
            }
        }

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
