package network;

import java.io.Serializable;

/**
 * Created as part of the class project for Mobile Computing
 */

public class NetworkPacket implements Serializable {
    private long id;
    private PacketType type;
    private int length;
    private byte[] data;
    private double latency;
    private String endPoint;

    public NetworkPacket() {

    }

    public NetworkPacket(long id, PacketType type, int length, byte[] data, String endPoint) {
        this.id = id;
        this.type = type;
        this.length = length;
        this.data = data;
        setEndPoint(endPoint);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }


    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }
}
