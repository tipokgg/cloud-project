public class ExceptionMessage extends AbstractMessage {

    private String exceptionText;

    public ExceptionMessage() {
    }

    public ExceptionMessage(String exceptionText) {
        this.exceptionText = exceptionText;
    }

    public String getExceptionText() {
        return exceptionText;
    }

    public void setExceptionText(String exceptionText) {
        this.exceptionText = exceptionText;
    }
}
