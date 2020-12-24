import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            String filePath = "cloud-server/files/user1/" + ((ChunkFileMessage) msg).getFile().getName(); // хардкод. куда пишем файл //TODO
            // записываем через RandomAccessFile, предвариьельно выбрав позицию, в какое место файла писать
            // информацию о позиции получаем из сериализованного объекта ChunkFileMessage
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                raf.seek(chunkFileMessage.getPosition());
                raf.write(chunkFileMessage.getData());
            }



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
