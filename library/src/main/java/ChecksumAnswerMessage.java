public class ChecksumAnswerMessage {

    boolean checksumValidation;

    public ChecksumAnswerMessage() {
    }

    public ChecksumAnswerMessage(boolean checksumValidation) {
        this.checksumValidation = checksumValidation;
    }

    public boolean isChecksumValidation() {
        return checksumValidation;
    }

    public void setChecksumValidation(boolean checksumValidation) {
        this.checksumValidation = checksumValidation;
    }

}
