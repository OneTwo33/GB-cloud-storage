package ru.onetwo33.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class NioTelnetServer {
    public static final String LS_COMMAND = "\tls - view all files and directories\n";
    public static final String MKDIR_COMMAND = "\tmkdir [dirname] - create directory\n";
    public static final String CHANGE_NICKNAME = "\tnick [nickname] - change nickname\n";
    public static final String TOUCH_COMMAND = "\ttouch [filename] - create file\n";
    public static final String CD_COMMAND = "\tcd [path] - move on catalog\n";
    public static final String RM_COMMAND = "\trm [filename | dirname] - delete file or directory (empty)\n";
    public static final String COPY_COMMAND = "\tcopy [src] [target] - copy file or directory\n";
    public static final String CAT_COMMAND = "\tcat [filename] - read file\n";

    private static final String ROOT_NOTIFICATION = "You are already in the root directory\n\n";
    private static final String DIRECTORY_DOESNT_EXIST = "Directory %s doesn't exist\n\n";
    private static final String ROOT_PATH = "server";

    private Path currentPath = Path.of("server");

    private Map<SocketAddress, String> clients = new HashMap<>();

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5678));
        server.configureBlocking(false);
        // OP_ACCEPT, OP_READ, OP_WRITE
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");

        while (server.isOpen()) {
            selector.select();

            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        SocketAddress client = channel.getRemoteAddress();

        String nickname = "";

        int readBytes = channel.read(buffer);
        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();

        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }

        buffer.clear();

        if (key.isValid()) {
            String command = sb
                    .toString()
                    .replace("\n", "")
                    .replace("\r", "");

            String[] cmds = command.split(" ");

            if ("--help".equals(cmds[0])) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
                sendMessage(CHANGE_NICKNAME, selector, client);
                sendMessage(TOUCH_COMMAND, selector, client);
                sendMessage(CD_COMMAND, selector, client);
                sendMessage(RM_COMMAND, selector, client);
                sendMessage(COPY_COMMAND, selector, client);
                sendMessage(CAT_COMMAND, selector, client);
            } else if ("ls".equals(cmds[0])) {
                sendMessage(getFileList().concat("\n"), selector, client);
            } else if ("touch".equals(cmds[0])) {
                createFile(cmds[1], selector, client);
            } else if ("mkdir".equals(cmds[0])) {
                createDirectory(cmds[1], selector, client);
            } else if ("cd".equals(cmds[0])) {
                replacePosition(selector, client, cmds[1]);
            } else if ("rm".equals(cmds[0])) {
                removeFile(cmds[1], selector, client);
            } else if ("copy".equals(cmds[0])) {
                copyFile(cmds[1], cmds[2], selector, client);
            } else if ("cat".equals(cmds[0])) {
                readFile(cmds[1], selector, client);
            } else if ("nick".equals(cmds[0])) {
                nickname = changeName(channel, cmds);
            } else if ("exit".equals(cmds[0])) {
                System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                channel.close();
                return;
            }
        }
        sendName(channel, nickname);
    }

    private void readFile(String filename, Selector selector, SocketAddress client) {
        Path path = Path.of(currentPath.toString(), filename);
        try {
            if (Files.isDirectory(path)) {
                sendMessage(String.format("%s is directory\n", filename), selector, client);
            } else if (Files.exists(path)) {
                FileChannel channel = new RandomAccessFile(path.toString(), "r").getChannel();
                int bytesRead = channel.read(buffer);
                while (bytesRead != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        sendMessage(String.valueOf((char) buffer.get()), selector, client);
                    }
                    buffer.clear();
                    bytesRead = channel.read(buffer);
                }
                sendMessage("\n", selector, client);
                channel.close();
            } else {
                sendMessage("File not found\n", selector, client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(String source, String target, Selector selector, SocketAddress client) throws IOException {
        Path pathSource = Path.of(currentPath.toString(), source);
        Path pathTarget = Path.of(currentPath.toString(), target);
        if (Files.isDirectory(pathSource)) {
            try {
                Files.walkFileTree(pathSource, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path newdir = pathTarget.resolve(pathSource.relativize(dir));
                        if (Files.exists(newdir)) {
                            return FileVisitResult.CONTINUE;
                        }
                        System.out.println("created dir: " + dir.toString());
                        Files.createDirectory(newdir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        System.out.println("copy file: " + file.toString());
                        Path newfile = pathTarget.resolve(pathSource.relativize(file));
                        Files.copy(file, newfile);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (FileAlreadyExistsException e) {
                sendMessage("File already exists\n", selector, client);
            } catch(IOException e){
                e.printStackTrace();
            }
        } else {
            try {
                Files.copy(pathSource, pathTarget);
            } catch (FileAlreadyExistsException e) {
                sendMessage("File already exists\n", selector, client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeFile(String filename, Selector selector, SocketAddress client) throws IOException {
        Path path = Path.of(currentPath.toString(), filename);
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                sendMessage("OK\n", selector, client);
            } else {
                sendMessage("File not found\n", selector, client);
            }
        } catch (DirectoryNotEmptyException e) {
            sendMessage(String.format("Directory %s not empty\n", filename), selector, client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDirectory(String dirname, Selector selector, SocketAddress client) {
        Path path = Path.of(currentPath.toString(), dirname);
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
                sendMessage("OK\n", selector, client);
            } else {
                sendMessage("File already exists\n", selector, client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void replacePosition(Selector selector, SocketAddress client, String neededPathString) throws IOException {
        Path tempPath = Path.of(currentPath.toString(), neededPathString);
        if ("..".equals(neededPathString)) {
            tempPath = currentPath.getParent(); // server/..
            if (tempPath == null || !tempPath.toString().startsWith("server")) {
                sendMessage(ROOT_NOTIFICATION, selector, client);
            } else {
                currentPath = tempPath;
            }
        } else if ("~".equals(neededPathString)) {
            currentPath = Path.of(ROOT_PATH);
        } else if (".".equals(neededPathString)) {
            // nothing to do
        } else {
            if (tempPath.toFile().exists()) {
                currentPath = tempPath;
            } else {
                sendMessage(String.format(DIRECTORY_DOESNT_EXIST, neededPathString), selector, client);
            }
        }
    }

    private String changeName(SocketChannel channel, String[] cmds) throws IOException {
        String nickname;
        nickname = cmds[1];
        clients.put(channel.getRemoteAddress(), nickname);
        System.out.println("Client - " + channel.getRemoteAddress().toString() + " changed nickname on " + nickname);
        System.out.println(clients);
        return nickname;
    }

    private void sendName(SocketChannel channel, String nickname) throws IOException {
        if (nickname.isEmpty()) {
            nickname = clients.getOrDefault(channel.getRemoteAddress(), channel.getRemoteAddress().toString());
        }
        String currentPathString = currentPath.toString().replace("server", "~");

        channel.write(
                ByteBuffer.wrap(nickname.concat(">:").concat(currentPathString).concat("$ ")
                .getBytes(StandardCharsets.UTF_8)
        ));

    }

    private void createFile(String filename, Selector selector, SocketAddress client) {
        Path path = Path.of(currentPath.toString(), filename);
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
                sendMessage("OK\n", selector, client);
            } else {
                sendMessage("File already exists\n", selector, client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileList() {
        return String.join(" ", new File(currentPath.toString()).list());
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel)key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel)key.channel())
                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());

        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap("Hello user!\n".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n".getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}