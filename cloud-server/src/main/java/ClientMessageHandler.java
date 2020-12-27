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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);
    private static final ArrayList<String> testUsers = new ArrayList<>(Arrays.asList("antiv18", "user1"));
    private static final ConcurrentHashMap<ChannelHandlerContext, AuthorizedClient> users = new ConcurrentHashMap();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {

        if (msg instanceof RequestAuthToServerMessage) { // авторизация
            String userName = ((RequestAuthToServerMessage) msg).getUserName();
            if (testUsers.contains(userName)) {
                users.put(ctx, new AuthorizedClient(userName));
                ctx.writeAndFlush(new FileListMessage(getUserFiles(ctx)));
            } else {
                ctx.writeAndFlush(new FailAuthMessage(userName)); // если ошибка авторизации
            }
        } else if (msg instanceof InitFileTransferMessage) { //пришёл запрос на upload от клиента
            InitFileTransferMessage init = (InitFileTransferMessage) msg;
            ctx.writeAndFlush(new ReadyForUploadMessage(init.getFile(), new File(users.get(ctx).getCurrentDir() + init.getFileName())));
        } else if (msg instanceof ChunkFileMessage) { // пришёл чанк от клиента
            ChunkFileMessage chunk = (ChunkFileMessage) msg;
            try (RandomAccessFile raf = new RandomAccessFile(chunk.getReceiverFile(), "rw")) {
                raf.seek(chunk.getPosition());
                raf.write(chunk.getData());
            }
        } else if (msg instanceof RequestFileDownloadMessage) { // если  пришёл запрос на загрузку с сервера
            sendFileToClient((RequestFileDownloadMessage) msg, ctx);
        } else if (msg instanceof FileListRequestMessage) {
            ctx.writeAndFlush(new FileListMessage(getUserFiles(ctx)));
        } else if (msg instanceof RequestFileDeleteMessage) {
            String fileName = ((RequestFileDeleteMessage) msg).getFileName();
            Files.deleteIfExists(Paths.get(users.get(ctx).getCurrentDir() + fileName));
            ctx.writeAndFlush(new FileListMessage(getUserFiles(ctx)));
        } else LOG.error("ERROR. Unknown message type!");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Client connected!");
    }


    private List<String> getUserFiles(ChannelHandlerContext ctx) throws IOException {
        Path clientDir = Paths.get(users.get(ctx).getCurrentDir());
        return  Files.list(clientDir)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    private void sendFileToClient(RequestFileDownloadMessage msg, ChannelHandlerContext ctx) throws IOException {

        String fileName = msg.getFileName();
        File serverSideFile = new File(users.get(ctx).getCurrentDir() + fileName);
        File clientSideFile = msg.getClientSideFile();
        String md5;

        try (InputStream is = Files.newInputStream(Paths.get(serverSideFile.toURI()))) {
            md5 = DigestUtils.md5Hex(is);
            System.out.println("checksum: " + md5);
        }

        long countOfChunks = serverSideFile.length()/1024 + 1;

        new Thread(() -> {
            // устанавливаем начальную позичию, откуда читать файл
            int currentPosition = 0;
            for (int i = 0; i < countOfChunks; i++) {
                if (i != countOfChunks -1) {
                    ctx.writeAndFlush(new ChunkFileMessage(clientSideFile, serverSideFile, currentPosition, 1024));
                    ctx.flush();
                    currentPosition += 1024; // после итерации увеличиваем позицию откуда читать байты на размер чанка
                } else {
                    ctx.writeAndFlush(new ChunkFileMessage(clientSideFile, serverSideFile, currentPosition,(int) serverSideFile.length() - currentPosition));
                    ctx.writeAndFlush(new UpdateClientSideMessage()); // чтоьбы клиент обновил список файлов у себя
                    ctx.flush();
                }
            }

        }).start();
    }
}
