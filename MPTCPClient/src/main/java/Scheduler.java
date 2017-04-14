import java.util.HashMap;
import java.util.Map;

/**
 * Created as part of the class project for Mobile Computing
 */
public class Scheduler {
    private String owner;
    private long offset;
    private boolean transferFinished = false;
    private int packetSize = 100;
    private int lteBandWidth = 50;
    private int wifiBandWidth = 75;
    private HashMap<SocketEndPoint, Long> latencyMap;

    public Scheduler(){
        latencyMap = new HashMap<SocketEndPoint, Long>();

        //latency = (packetSize / delay) - bandWidth;


    }

    public SocketEndPoint getScheduledEndPoint(){
        Map.Entry<SocketEndPoint,Long> min = null;
        for(Map.Entry<SocketEndPoint, Long> entry: latencyMap.entrySet()){
            if(min == null){
                min = entry;
                continue;
            }
            if(min.getValue() > entry.getValue())
            {
                min = entry;
            }
        }
        return min.getKey();
    }

    public void addToTable(SocketEndPoint key, long val){
        latencyMap.put(key, calcLatency(val));
    }

    public void updateTable(SocketEndPoint key, long val)
    {
        latencyMap.replace(key,calcLatency(val));
    }

    private long calcLatency(long delay){
        return delay;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public boolean isTransferFinished() {
        return transferFinished;
    }

    public void setTransferFinished(boolean transferFinished) {
        this.transferFinished = transferFinished;
    }


}
