/**
 * Created as part of the class project for Mobile Computing
 */
public class ClientApplication {

    public static String storePath = System.getProperty("user.dir");


    public static void main(String[] args) throws InterruptedException {
        Scheduler scheduler = new Scheduler();
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
    }
}
