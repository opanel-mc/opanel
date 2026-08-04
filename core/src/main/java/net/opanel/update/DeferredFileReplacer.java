package net.opanel.update;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies a staged plugin update from a child JVM after the server process exits.
 * This is used by legacy platforms that cannot load the Java 17 file-operations helper.
 */
public final class DeferredFileReplacer {
    private static final int RETRY_COUNT = 1200;
    private static final long RETRY_INTERVAL_MS = 250;

    private DeferredFileReplacer() {}

    public static void scheduleMove(Path source, Path target) throws IOException {
        final Path javaExecutable = Path.of(
            System.getProperty("java.home"),
            "bin",
            isWindows() ? "java.exe" : "java"
        );
        if(!Files.isRegularFile(javaExecutable)) {
            throw new IOException("Cannot find the Java executable for the deferred plugin update.");
        }

        final Path classPath;
        try {
            classPath = Path.of(
                DeferredFileReplacer.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
        } catch (NullPointerException | URISyntaxException e) {
            throw new IOException("Cannot locate OPanel for the deferred plugin update.", e);
        }

        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.add("-cp");
        command.add(classPath.toString());
        command.add(DeferredFileReplacer.class.getName());
        command.add(Long.toString(ProcessHandle.current().pid()));
        command.add(source.toAbsolutePath().normalize().toString());
        command.add(target.toAbsolutePath().normalize().toString());

        new ProcessBuilder(command)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
    }

    public static void main(String[] args) {
        if(args.length != 3) return;

        final long parentPid;
        try {
            parentPid = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            return;
        }

        Optional<ProcessHandle> parent = ProcessHandle.of(parentPid);
        parent.ifPresent(process -> process.onExit().join());
        replaceWithRetries(Path.of(args[1]), Path.of(args[2]));
    }

    static boolean replaceWithRetries(Path source, Path target) {
        for(int attempt = 0; attempt < RETRY_COUNT; attempt++) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }
}
