import java.io.*;

public class ChunkFileMessage extends AbstractMessage {

    // класс который генерирует чанки

    private File file; // сам файл
    private int chunkSize; // размер чанка
    private String nameOfChunkedFile; // пока не испольуется
    private byte[] data; // буффер, куда будут записаны байты из файла
    private int position; // позиция, с которой будут читаться байты из файла

    public ChunkFileMessage() {
    }


    public ChunkFileMessage(File file, int position, long chunkSize) {
        this.chunkSize = (int) chunkSize; // заполняем длину чанка
        data = new byte[this.chunkSize]; // создаем буффер нужного размера
        this.file = file;
        this.position = position;
        fillArray(this.position); // вызываем метод, который заполнит буффер
    }


    public void fillArray(int position) {
        // открыаем RandomAccessFile
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // устаналивем указатель на нужную позицию
            raf.seek(position);
            // и читаем в буффер
            raf.read(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
