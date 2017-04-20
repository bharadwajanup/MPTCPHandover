import network.SocketEndPoint;

import java.io.IOException;
import java.util.ArrayList;
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
    private int windowSize = 100;
    private HashMap<SocketEndPoint, Double> latencyMap;
    private HashMap<SocketEndPoint, CircularArrayList<Double>> rttMap;
    private double Wifi;
    private double Lte;

    public Scheduler(){
        latencyMap = new HashMap<SocketEndPoint, Double>();
        rttMap = new HashMap<SocketEndPoint, CircularArrayList<Double>>();
    }

    public SocketEndPoint getScheduledEndPoint() throws IOException {
        //Map.Entry<SocketEndPoint,CircularArrayList> min = null;
        /*
        for(Map.Entry<SocketEndPoint, Double> entry: latencyMap.entrySet()){
            if(min == null){
                min = entry;
                continue;
            }
            if(min.getValue() > entry.getValue()) {
                min = entry;
            }
        }
        return min.getKey();
        */
        double minVal = Double.MAX_VALUE;
        SocketEndPoint min = null;
        double wifiAvg = 0;
        double lteAvg = 0;
        for(Map.Entry<SocketEndPoint, CircularArrayList<Double>> rtt: rttMap.entrySet()){
            CircularArrayList<Double> arr = rtt.getValue();
            if(rtt.getKey().getEndPointName().equals("Wi-Fi")) {
                int i;
                for (i = 0; i < arr.size(); i ++) {
                    wifiAvg += arr.get(i);
                }
                Wifi = wifiAvg/arr.size();
                if (Wifi < minVal) {
                    minVal = Wifi;
                    min = rtt.getKey();
                }
            }
            else{
                int i;
                for (i = 0; i < arr.size(); i ++) {
                    lteAvg += arr.get(i);
                }
                Lte = lteAvg/arr.size();
                if(Lte < minVal){
                    minVal = Lte;
                    min = rtt.getKey();
                }
            }

        }
        //String min = (Wifi < Lte)? "Wi-Fi" : "LTE";
        return min;

    }

    public void addToTable(SocketEndPoint key, double val){
        latencyMap.put(key, val);
        updateTable(key,val);
    }

    public void updateTable(SocketEndPoint key, double val) {
        CircularArrayList<Double> rttPoints;
        if(rttMap.get(key) == null) {
            rttPoints = new CircularArrayList<Double>(windowSize);
            rttMap.put(key, rttPoints);
        }
        else {
            rttPoints = rttMap.get(key);
        }
        rttPoints.add(rttPoints.size(), val);
        rttMap.replace(key, rttPoints);
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
