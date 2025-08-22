package net.opanel.utils;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.*;

public class ZipUtility {
    private static final int BUFFER_SIZE = 8192; // 8 KB

    public static void zip(Path sourceDirPath, Path zipPath) throws IOException {
        File sourceDir = new File(sourceDirPath.toString());
        if(!sourceDir.exists()) {
            throw new IOException("Cannot find the source directory.");
        }

        try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toString()))) {
            addFolderToZos(sourceDir, sourceDir.getName(), zos);
        }
    }

    // for recursively calling
    private static void addFolderToZos(File folder, String parentDirName, ZipOutputStream zos) throws IOException {
        if(!folder.exists()) {
            throw new IOException("Folder not found: "+ folder.getPath());
        }

        for(File item : folder.listFiles()) {
            // this file can be inaccessible when another thread or program is using the folder
            // so just simply skip it...
            if(item.getName().endsWith("session.lock")) continue;
            if(item.isDirectory()) {
                addFolderToZos(item, parentDirName +"/"+ item.getName(), zos);
            } else {
                final String entryName = parentDirName +"/"+ item.getName();
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);

                try(FileInputStream fis = new FileInputStream(item)) {
                    byte[] bytesIn = new byte[BUFFER_SIZE];
                    int read;
                    while((read = fis.read(bytesIn)) != -1) {
                        zos.write(bytesIn, 0, read);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    public static void unzip(Path zipPath, Path targetDirPath) throws IOException {
        File targetDir = new File(targetDirPath.toString());
        if(!targetDir.exists() && !targetDir.mkdir()) {
            throw new IOException("Cannot create target directory.");
        }

        try(ZipInputStream zin = new ZipInputStream(new FileInputStream(zipPath.toString()))) {
            ZipEntry entry = zin.getNextEntry();
            while(entry != null) {
                Path filePath = targetDirPath.resolve(entry.getName());
                /** @todo prevent zip slip */
                // ...
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
