package Network;

import java.io.Serializable;

/**
 * Created by Bharadwaj on 4/10/2017.
 */
public enum PacketType implements Serializable {
    DATA,
    ACKNOWLEDGEMENT,
    CLOSE_INDICATOR,
    INITIALIZER
}