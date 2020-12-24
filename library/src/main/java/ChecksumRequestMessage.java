public class ChecksumRequestMessage extends AbstractMessage {

    private String checksum;

    public ChecksumRequestMessage() {
    }

    public ChecksumRequestMessage(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

}
