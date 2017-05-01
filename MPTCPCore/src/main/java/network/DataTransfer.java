package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

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

    public boolean sendData(NetworkPacket packet) throws IOException {
        try {
            getOutputStream().writeObject(packet);
            return true;
        } catch (Exception ex) {
            System.out.println("Write Failed due to an error.");
            return false;
        }

    }

    public NetworkPacket receiveData() throws Exception {
        try {
            return (NetworkPacket) getInputStream().readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;

        } catch (SocketTimeoutException s) {
            return null;
        }
    }

    public void close() throws IOException {
        if (!getSocket().isClosed()) {
//            getInputStream().close();
//            getOutputStream().close();
            getSocket().close();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    private void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectInputStream getInputStream() {
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
