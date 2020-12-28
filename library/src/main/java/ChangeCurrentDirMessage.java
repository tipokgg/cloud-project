public class ChangeCurrentDirMessage extends AbstractMessage {

    private String newDirName;

    public ChangeCurrentDirMessage() {
    }

    public ChangeCurrentDirMessage(String newDirName) {
        this.newDirName = newDirName;
    }

    public String getNewDirName() {
        return newDirName;
    }

    public void setNewDirName(String newDirName) {
        this.newDirName = newDirName;
    }
}
