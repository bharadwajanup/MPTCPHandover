package Network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created as part of the class project for Mobile Computing
 */
public class DataTransfer {

    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public DataTransfer(Socket socket) throws IOException {
        setSocket(socket);
        //Always set the Output Stream first before input stream to avoid deadlocks!
        setOutputStream(new ObjectOutputStream(socket.getOutputStream()));
        setInputStream(new ObjectInputStream(socket.getInputStream()));
    }

    public void sendData(NetworkPacket packet) throws IOException {
        getOutputStream().writeObject(packet);
    }

    public NetworkPacket receiveData() throws IOException {
        try {
            return (NetworkPacket) getInputStream().readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() throws IOException {
        getInputStream().close();
        getOutputStream().close();
        getSocket().close();
    }

    public Socket getSocket() {
        return socket;
    }

    private void setSocket(Socket socket) {
        this.socket = socket;
    }

    private ObjectInputStream getInputStream() {
        return inputStream;
    }

    private void setInputStream(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
    }

    private ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    private void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }
}
