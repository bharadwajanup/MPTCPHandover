/**
 * Created as part of the class project for Mobile Computing
 */
public class Scheduler {
    private String owner;
    private long offset;
    private boolean transferFinished = false;

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
