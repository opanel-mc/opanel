package net.opanel.utils;

import org.eclipse.jetty.util.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.*;

public class ZipUtility {
    private static final int BUFFER_SIZE = 8192; // 8 KB

    private final Path zipPath;
    private final Path targetDirPath;

    public ZipUtility(Path zipPath, Path targetDirPath) {
        this.zipPath = zipPath;
        this.targetDirPath = targetDirPath;
    }

    public void unzip() throws IOException {
        File targetDir = new File(targetDirPath.toString());
        if(!targetDir.exists() && !targetDir.mkdir()) {
            throw new IOException("Cannot create target directory.");
        }

        try(ZipInputStream zin = new ZipInputStream(new FileInputStream(zipPath.toString()))) {
            ZipEntry entry = zin.getNextEntry();
            while(entry != null) {
                Path filePath = targetDirPath.resolve(entry.getName());
                if(!entry.isDirectory()) {
                    /** @see https://baeldung.com/java-compress-and-uncompress#unzip */
                    File parentDir = new File(filePath.toString()).getParentFile();
                    if(!parentDir.exists() && !parentDir.mkdir()) {
                        throw new IOException("Cannot create directory '"+ parentDir.getName() +"'.");
                    }

                    try(FileOutputStream fos = new FileOutputStream(filePath.toString())) {
                        byte[] bytesIn = new byte[BUFFER_SIZE];
                        int read;
                        while((read = zin.read(bytesIn)) != -1) {
                            fos.write(bytesIn, 0, read);
                        }
                    }
                } else {
                    File dir = new File(filePath.toString());
                    if(!dir.mkdir()) {
                        throw new IOException("Cannot create directory '"+ entry.getName() +"'.");
                    }
                }
                zin.closeEntry();
                entry = zin.getNextEntry();
            }
        }
    }
}
