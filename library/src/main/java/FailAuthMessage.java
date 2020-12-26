public class FailAuthMessage extends AbstractMessage {

    private String userName;

    public FailAuthMessage() {
    }

    public FailAuthMessage(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
