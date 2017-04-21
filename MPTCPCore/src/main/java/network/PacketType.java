package network;
/**
 * Created as part of the class project for Mobile Computing
 */
import java.io.Serializable;

public enum PacketType implements Serializable {
    DATA,
    ACKNOWLEDGEMENT,
    CLOSE_INDICATOR,
    INITIALIZER,
    PING

}