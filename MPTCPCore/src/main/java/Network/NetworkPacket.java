package Network;

import java.io.Serializable;

/**
 * Created as part of the class project for Mobile Computing
 */

public class NetworkPacket implements Serializable {
    private long id;
    private PacketType type;
    private int length;
    private byte[] data;

    public NetworkPacket() {

    }

    public NetworkPacket(long id, PacketType type, int length, byte[] data) {
        this.id = id;
        this.type = type;
        this.length = length;
        this.data = data;
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


}
