import network.DataTransfer;
import network.NetworkPacket;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created as part of the class project for Mobile Computing
 */
public class PacketSender implements Runnable {


    private final Lock lock;
    private final Condition condition;
    private BlockingQueue<NetworkPacket> queue;
    private DataTransfer fileTransfer;

    public PacketSender(BlockingQueue<NetworkPacket> queue, DataTransfer fileTransfer, Lock lock, Condition condition) {
        this.queue = queue;
        this.fileTransfer = fileTransfer;
        this.lock = lock;
        this.condition = condition;
    }

    @Override
    public void run() {
        while (true) {

            try {
                NetworkPacket packet = queue.take();

                lock.lock();
                boolean signalled = condition.await((long) packet.getLatency(), TimeUnit.MILLISECONDS);
                lock.unlock();

                if (signalled) {
                    queue.clear();
                    continue;
                }
                boolean noErrors = fileTransfer.sendData(packet);
                if (!noErrors)
                    break;
            } catch (InterruptedException e) {
                System.out.println("Sender Thread was Interrupted");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
