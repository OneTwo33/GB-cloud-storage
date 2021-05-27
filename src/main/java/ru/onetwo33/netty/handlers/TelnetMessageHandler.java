package ru.onetwo33.netty.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class TelnetMessageHandler extends SimpleChannelInboundHandler<String> {

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

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        ctx.writeAndFlush("Hello user!\n");
        ctx.writeAndFlush("Enter --help for support info\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        String nickname = "";

        if (!msg.isEmpty()) {

            String command = msg
                    .replace("\n", "")
                    .replace("\r", "");

            String[] cmds = command.split(" ");

            if ("--help".equals(cmds[0])) {
                sendMessage(LS_COMMAND, ctx);
                sendMessage(MKDIR_COMMAND, ctx);
                sendMessage(CHANGE_NICKNAME, ctx);
                sendMessage(TOUCH_COMMAND, ctx);
                sendMessage(CD_COMMAND, ctx);
                sendMessage(RM_COMMAND, ctx);
                sendMessage(COPY_COMMAND, ctx);
                sendMessage(CAT_COMMAND, ctx);
            } else if ("ls".equals(cmds[0])) {
                sendMessage(getFileList().concat("\n"), ctx);
            } else if ("touch".equals(cmds[0])) {
                createFile(cmds[1], ctx);
            } else if ("mkdir".equals(cmds[0])) {
                createDirectory(cmds[1], ctx);
            } else if ("cd".equals(cmds[0])) {
                replacePosition(ctx, cmds[1]);
            } else if ("rm".equals(cmds[0])) {
                removeFile(cmds[1], ctx);
            } else if ("copy".equals(cmds[0])) {
                copyFile(cmds[1], cmds[2], ctx);
            } else if ("cat".equals(cmds[0])) {
                readFile(cmds[1], ctx);
            } else if ("nick".equals(cmds[0])) {
                nickname = changeName(ctx, cmds);
            } else if ("exit".equals(cmds[0])) {
                System.out.println("Client logged out. IP: " + ctx.channel().remoteAddress());
                ctx.channel().close();
            }
        }
        sendName(ctx, nickname);
    }

    private void sendMessage(String message, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(message);
    }

    private void sendName(ChannelHandlerContext ctx, String nickname) {
        if (nickname.isEmpty()) {
            nickname = clients.getOrDefault(ctx.channel().remoteAddress(), ctx.channel().remoteAddress().toString());
        }
        String currentPathString = currentPath.toString().replace("server", "~");

        ctx.writeAndFlush(nickname.concat(">:").concat(currentPathString).concat("$ "));
    }

    private String changeName(ChannelHandlerContext ctx, String[] cmds) {
        String nickname;
        nickname = cmds[1];
        clients.put(ctx.channel().remoteAddress(), nickname);
        System.out.println("Client - " + ctx.channel().remoteAddress().toString() + " changed nickname on " + nickname);
        System.out.println(clients);
        return nickname;
    }

    private String getFileList() {
        return String.join(" ", new File(currentPath.toString()).list());
    }

    private void createFile(String filename, ChannelHandlerContext ctx) {
        Path path = Path.of(currentPath.toString(), filename);
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
                sendMessage("OK\n", ctx);
            } else {
                sendMessage("File already exists\n", ctx);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDirectory(String dirname, ChannelHandlerContext ctx) {
        Path path = Path.of(currentPath.toString(), dirname);
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
                sendMessage("OK\n", ctx);
            } else {
                sendMessage("File already exists\n", ctx);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void replacePosition(ChannelHandlerContext ctx, String neededPathString) {
        Path tempPath = Path.of(currentPath.toString(), neededPathString);
        if ("..".equals(neededPathString)) {
            tempPath = currentPath.getParent(); // server/..
            if (tempPath == null || !tempPath.toString().startsWith("server")) {
                sendMessage(ROOT_NOTIFICATION, ctx);
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
                sendMessage(String.format(DIRECTORY_DOESNT_EXIST, neededPathString), ctx);
            }
        }
    }

    private void removeFile(String filename, ChannelHandlerContext ctx) {
        Path path = Path.of(currentPath.toString(), filename);
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                sendMessage("OK\n", ctx);
            } else {
                sendMessage("File not found\n", ctx);
            }
        } catch (DirectoryNotEmptyException e) {
            sendMessage(String.format("Directory %s not empty\n", filename), ctx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(String source, String target, ChannelHandlerContext ctx) {
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
                sendMessage("File already exists\n", ctx);
            } catch(IOException e){
                e.printStackTrace();
            }
        } else {
            try {
                Files.copy(pathSource, pathTarget);
            } catch (FileAlreadyExistsException e) {
                sendMessage("File already exists\n", ctx);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readFile(String filename, ChannelHandlerContext ctx) {
        Path path = Path.of(currentPath.toString(), filename);
        try {
            if (Files.isDirectory(path)) {
                sendMessage(String.format("%s is directory\n", filename), ctx);
            } else if (Files.exists(path)) {
                FileChannel channel = new RandomAccessFile(path.toString(), "r").getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(512);
                int bytesRead = channel.read(buffer);
                while (bytesRead != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        sendMessage(String.valueOf((char) buffer.get()), ctx);
                    }
                    buffer.clear();
                    bytesRead = channel.read(buffer);
                }
                sendMessage("\n", ctx);
                channel.close();
            } else {
                sendMessage("File not found\n", ctx);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
