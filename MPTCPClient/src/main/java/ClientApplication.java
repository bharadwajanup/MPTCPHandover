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

    public static Tuple<NetworkPacket, Long> calculateLatency(SocketEndPoint endPoint, ExecutorService executor) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        Future<NetworkPacket> future = executor.submit((Callable) endPoint);
        NetworkPacket packet = future.get();
        long endTime = System.currentTimeMillis();
        return new Tuple<>(packet, endTime - startTime);
    }


    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        long startTime, endTime, totalTime;
        int perPacketSize = 100;
        String fileName = NetworkConfiguration.getProperty("file");
        long curAck;

       /* Scheduler scheduler = new Scheduler();
        Object schedulerCall = new Object();
        scheduler.setOffset(1);
        String socketOwner = "WI-FI";
        scheduler.setOwner(socketOwner);

        Thread wifiThread = new Thread(new SocketThread(schedulerCall, scheduler, storePath + "\\" + "wifi\\", "WI-FI"));
        wifiThread.setName("WI-FI");
        Thread lteThread = new Thread(new SocketThread(schedulerCall, scheduler, storePath + "\\" + "wifi\\", "LTE"));
        lteThread.setName("LTE");
        wifiThread.start();
        lteThread.start();


        while (!scheduler.isTransferFinished()) {
            synchronized (scheduler) {
                while (!scheduler.getOwner().equals("SCH"))
                    scheduler.wait();
            }
            socketOwner = socketOwner.equals("LTE") ? "WI-FI" : "LTE";
            synchronized (scheduler) {
                System.out.println("Scheduler changing Owner to " + socketOwner);
                scheduler.setOwner(socketOwner);
                scheduler.notifyAll();
            }
            synchronized (scheduler) {
                scheduler.notifyAll();
            }

        }


        wifiThread.join();
        lteThread.join();
        */
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
        Tuple<NetworkPacket, Long> result = calculateLatency(wifiPacket, executor);
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
