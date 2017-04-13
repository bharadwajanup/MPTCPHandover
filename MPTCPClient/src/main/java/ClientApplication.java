/**
 * Created as part of the class project for Mobile Computing
 */
public class ClientApplication {

    public static String storePath = System.getProperty("user.dir");

    public static void main(String[] args) throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        scheduler.setOffset(1);
        scheduler.setOwner("WI-FI");
        Thread wifiThread = new Thread(new SocketThread(ClientApplication.class, scheduler, storePath + "\\" + "wifi\\", "WI-FI"));
        wifiThread.setName("WI-FI");
        Thread lteThread = new Thread(new SocketThread(ClientApplication.class, scheduler, storePath + "\\" + "lte\\", "LTE"));
        lteThread.setName("LTE");
        wifiThread.start();
        lteThread.start();
        synchronized (wifiThread) {
            wifiThread.wait();
            scheduler.setOwner("LTE");
            scheduler.setOffset(1);
            synchronized (ClientApplication.class) {
                ClientApplication.class.notifyAll();
            }

            //wifiThread.notify();
        }


        wifiThread.join();
        lteThread.join();
    }
}
