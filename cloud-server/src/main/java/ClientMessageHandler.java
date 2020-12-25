import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {

        // проверяем что за объект пришёл
        if (msg instanceof InitFileTransferMessage) {
            LOG.info("Received: " + msg);
            // если пришёл запрос на передачу файла от клиента, отправяем сер. объект о готвности приёма
            ctx.writeAndFlush(new ReadyForUploadMessage(((InitFileTransferMessage) msg).getFile()));
            // если это чанк с данными
        } else if (msg instanceof ChunkFileMessage) {
            // то сначала запишем в переменную
            ChunkFileMessage chunkFileMessage = (ChunkFileMessage) msg;
            // выводы в консоль для проверок, что происходит
            String filePath = "cloud-server/files/user1/" + ((ChunkFileMessage) msg).getFile().getName(); // хардкод. куда пишем файл //TODO получать userName
            // записываем через RandomAccessFile, предвариьельно выбрав позицию, в какое место файла писать
            // информацию о позиции получаем из сериализованного объекта ChunkFileMessage
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                raf.seek(chunkFileMessage.getPosition());
                raf.write(chunkFileMessage.getData());
            }
        } else if (msg instanceof RequestFileDownloadMessage) {
            String fileName = ((RequestFileDownloadMessage) msg).getFileName();
            String userName = ((RequestFileDownloadMessage) msg).getUserName();
            File fileForUpload = new File("cloud-server/files/" + userName + "/" + fileName);

            try (InputStream is = Files.newInputStream(Paths.get(fileForUpload.toURI()))) {
                String md5 = DigestUtils.md5Hex(is);
                System.out.println("checksum: " + md5);
            }

            System.out.println("Длина файла в байтах - " + fileForUpload.length());
            long countOfChunks = fileForUpload.length()/1024 + 1;
            System.out.println("Будет передано чанков - " + countOfChunks);


            new Thread(() -> {
                // устанавливаем начальную позичию, откуда читать файл
                int currentPosition = 0;
                // в цикле начинаем генерировать чанки, которые являются сериализованными объектами и отправлять на сервер
                // в чанках есть буффер с данными, вычитанными из файла, что это за файл, какой размер чанка и из какой позиции читались байты
                for (int i = 0; i < countOfChunks; i++) {
                    if (i != countOfChunks -1) {
                        ctx.writeAndFlush(new ChunkFileMessage(fileForUpload, currentPosition, 1024));
                        ctx.flush();
                        currentPosition += 1024; // после итерации увеличиваем позицию откуда читать байты на размер чанка
                        // в последней итерации создаем чанк, равный количеству байт, который осталось считать из файла до конца,
                        // чтобы не делать полный чанк длиной 1024 байта
                    } else {
                        ctx.writeAndFlush(new ChunkFileMessage(fileForUpload, currentPosition,fileForUpload.length() - currentPosition));
                        ctx.writeAndFlush(new UpdateClientSideMessage()); // чтоьбы клиент обновил список файлов у себя
                        ctx.flush();
                    }
                }

            }).start();

        } else if (msg instanceof FileListRequestMessage) {

            String userName = ((FileListRequestMessage) msg).getUsername();
            Path clientDir = Paths.get("cloud-server/files/" + userName);
            List<String> clientFiles;

            clientFiles = Files.list(clientDir)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            ctx.writeAndFlush(new FileListMessage(clientFiles));

            LOG.info("Sent list of files for: " + userName);
        }

        else LOG.error("ERROR");

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Client connected!");
    }
}
