import common.Tuple;
import network.NetworkConfiguration;
import network.NetworkPacket;
import network.PacketType;
import network.SocketEndPoint;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.*;

/**
 * Created as part of the class project for Mobile Computing
 */
public class ClientApplication {

    private static String storePath = System.getProperty("user.dir");

    @Deprecated
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


    private static Future<Tuple<NetworkPacket, Double>> getResult(long id, Scheduler.Flow flow, PacketType type, ExecutorService executor) throws SocketException {
        SocketEndPoint sendingEndPoint = flow.getEndPoint();
//        System.out.println(flow.getEndPoint().getEndPointName()+" "+flow.getExpectation());
        int timeout = (int) flow.getExpectation();

        NetworkPacket packet = new NetworkPacket(id, type, 0, null, sendingEndPoint.getEndPointName());
//        System.out.println(packet);
        return getFuture(executor, sendingEndPoint, packet, timeout);
    }

    private static Future<Tuple<NetworkPacket, Double>> getFuture(ExecutorService executor, SocketEndPoint callableEndPoint, NetworkPacket packet, int timeout) throws SocketException {
        callableEndPoint.setNetworkPacket(packet);
        callableEndPoint.setTimeout(timeout);
        return executor.submit(callableEndPoint);
    }


    private static void initializeFlow(ExecutorService executor, SocketEndPoint endPoint, NetworkPacket initializer, Scheduler scheduler, boolean main) throws SocketException, ExecutionException, InterruptedException {
        Future<Tuple<NetworkPacket, Double>> future = getFuture(executor, endPoint, initializer, 0);
        Tuple<NetworkPacket, Double> result = future.get();
        scheduler.setFlow(endPoint, result.y, main);
    }


    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        long startTime, endTime;
        int perPacketSize = 100;
        String fileName = NetworkConfiguration.getProperty("file");
        long curAck;

        Future<Tuple<NetworkPacket, Double>> mainFlowResult;
        Future<Tuple<NetworkPacket, Double>> subFlowResult;
        Tuple<NetworkPacket, Double> result;

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
//        SocketEndPoint sendingEndPoint;

        //Start Packet Downloader.
        ArrayBlockingQueue<NetworkPacket> blockQueue = new ArrayBlockingQueue<>(1024);
        PacketDownloader downloadPacket = new PacketDownloader(blockQueue, storePath + "/" + fileName);
        executor.execute(downloadPacket);

        packet.setEndPoint(wifiPacket.getEndPointName());
        initializeFlow(executor, wifiPacket, packet, scheduler, true);

        packet.setEndPoint(ltePacket.getEndPointName());
        initializeFlow(executor, ltePacket, packet, scheduler, false);

        scheduler.getSubFlow().setExpectation(0);

        curAck = packet.getId();

        startTime = System.currentTimeMillis();

        int i = 0, j = 0;
        while (true) {

//            System.out.println("MainFlow: "+scheduler.getMainFlow().getEndPoint().getEndPointName());
            mainFlowResult = getResult(curAck, scheduler.getMainFlow(), scheduler.getPacketType(true), executor);
//            System.out.println("SubFlow: "+scheduler.getSubFlow().getEndPoint().getEndPointName());
            subFlowResult = getResult(curAck, scheduler.getSubFlow(), scheduler.getPacketType(false), executor);

            result = mainFlowResult.get();
            scheduler.updateMainFlow(result.y);

            if (result.x == null || result.x.getType() != PacketType.PING)
                packet = result.x;

            result = subFlowResult.get();
            if (result != null) {
                scheduler.updateSubFlow(result.y);

                if (result.x == null || result.x.getType() != PacketType.PING)
                    packet = result.x;
            }


            if (packet == null) {
                scheduler.setFlow(false);
                continue;
            }
            endTime = System.currentTimeMillis();

            if (packet.getType() == PacketType.CLOSE_INDICATOR)
                break;

            if (packet.getId() >= curAck && packet.getType() == PacketType.DATA) {
                System.out.println(String.format("%d %d %s", packet.getId(), (endTime - startTime), packet.getEndPoint()));
                curAck = packet.getId() + perPacketSize;
                blockQueue.add(packet);
                startTime = System.currentTimeMillis();
            }
        }
        System.out.println("Wifi: " + i);
        System.out.println("Lte: " + j);
        System.out.println("File Downloaded...");

        scheduler.closeFlows();


        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);

        if (!terminated)
            executor.shutdownNow();
    }

    private static void printRTTLog(SocketEndPoint sendingEndPoint, Double y) {
        String message = String.format("%s\t%f", sendingEndPoint.getEndPointName(), y);
        System.out.println(message);
    }
}
