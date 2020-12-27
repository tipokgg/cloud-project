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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);
    private static final ArrayList<String> testUsers = new ArrayList<>(Arrays.asList("antiv18", "user1"));
    private static final ConcurrentHashMap<ChannelHandlerContext, AuthorizedClient> users = new ConcurrentHashMap();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {

        if (msg instanceof RequestAuthToServerMessage) { // авторизация
            String userNameForCheck = ((RequestAuthToServerMessage) msg).getUserName();
            String passForCheck = ((RequestAuthToServerMessage) msg).getPass();
            String userName = SQLConnector.getNickname(userNameForCheck, passForCheck);
            if (userName != null) {
                users.put(ctx, new AuthorizedClient(userName));
                ctx.writeAndFlush(new SuccessAuthMessage(getUserFiles(ctx)));
                LOG.info("Success auth from user: " + userName);
            } else {
                ctx.writeAndFlush(new FailAuthMessage(userNameForCheck)); // если ошибка авторизации
                LOG.info("Fail auth from user: " + userNameForCheck);
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
            if (((RequestFileDownloadMessage) msg).getFileName().startsWith("<DIR> ")) {
                ctx.writeAndFlush(new ExceptionMessage(((RequestFileDownloadMessage) msg).getFileName() +
                        " is a directory!"));
            } else sendFileToClient((RequestFileDownloadMessage) msg, ctx);
        } else if (msg instanceof FileListRequestMessage) {
            ctx.writeAndFlush(new FileListMessage(getUserFiles(ctx)));
        } else if (msg instanceof RequestFileDeleteMessage) {
            String fileName = ((RequestFileDeleteMessage) msg).getFileName();
            Files.deleteIfExists(Paths.get(users.get(ctx).getCurrentDir() + fileName));
            ctx.writeAndFlush(new FileListMessage(getUserFiles(ctx)));
        } else if (msg instanceof DisconnectMessage) {
            LOG.info("User " + users.get(ctx).getUserName() + " disconnected from the server");
            users.remove(ctx);
        } else LOG.error("ERROR. Unknown message type!");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Client connected!");
    }


    private List<String> getUserFiles(ChannelHandlerContext ctx) {

        File[] files = Objects.requireNonNull(new File(users.get(ctx).getCurrentDir()).listFiles());
        List<String> filesString = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                filesString.add("<DIR> " + file.getName());
            } else {
                filesString.add(file.getName());
            }
        }

        return filesString;

    }

    private void sendFileToClient(RequestFileDownloadMessage msg, ChannelHandlerContext ctx) throws IOException {

        String fileName = msg.getFileName();
        File serverSideFile = new File(users.get(ctx).getCurrentDir() + fileName);
        File clientSideFile = msg.getClientSideFile();
        String md5;

        try (InputStream is = Files.newInputStream(Paths.get(serverSideFile.toURI()))) {
            md5 = DigestUtils.md5Hex(is);
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
