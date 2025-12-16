package net.opanel.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class Utils {
    /**
     * @see <a href="https://ithelp.ithome.com.tw/articles/10212717">https://ithelp.ithome.com.tw/articles/10212717</a>
     */
    public static String md5(String str) {
        if(str == null || str.isEmpty()) return "";
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest();
            
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for(byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute MD5 hash", e);
        }
    }

    public static String stringToBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64ToString(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public static byte[] readFile(Path filePath) throws IOException {
        if(!Files.exists(filePath)) {
            throw new IOException("Cannot find the specified file.");
        }

        final int BUFFER_SIZE = 8192; // 8 KB
        try(
                FileInputStream fis = new FileInputStream(filePath.toString());
                BufferedInputStream bis = new BufferedInputStream(fis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        }
    }

    public static String readTextFile(Path filePath) throws IOException {
        return new String(readFile(filePath), StandardCharsets.UTF_8);
    }

    public static void writeTextFile(Path filePath, String content) throws IOException {
        Files.writeString(filePath, content, StandardCharsets.UTF_8, 
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static String decompressTextGzip(Path gzipPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(
                FileInputStream fis = new FileInputStream(gzipPath.toString());
                GZIPInputStream gis = new GZIPInputStream(fis);
                InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)
        ) {
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static void deleteDirectoryRecursively(Path dirPath) throws IOException {
        if(!Files.exists(dirPath)) {
            throw new IOException("The given path doesn't exist.");
        }
        Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path subDir, IOException e) throws IOException {
                if(e != null) throw e;
                Files.delete(subDir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void clearDirectoryRecursively(Path dirPath) throws IOException {
        if(!Files.exists(dirPath)) {
            throw new IOException("The given path doesn't exist.");
        }
        Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path subDir, IOException e) throws IOException {
                if(e != null) throw e;
                if(!subDir.equals(dirPath)) Files.delete(subDir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyDirectoryRecursively(Path sourcePath, Path targetPath) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path subDir, BasicFileAttributes attr) throws IOException {
                try {
                    Files.copy(subDir, targetPath.resolve(sourcePath.relativize(subDir)), StandardCopyOption.COPY_ATTRIBUTES);
                } catch (FileAlreadyExistsException e) {
                    //
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static long getDirectorySize(Path dirPath) throws IOException {
        if(!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IOException("Cannot find the directory.");
        }

        try(Stream<Path> stream = Files.walk(dirPath)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            // Log error but continue processing other files
                            System.err.println("Failed to get size of file: " + path + ", " + e.getMessage());
                            return 0L;
                        }
                    })
                    .sum();
        }
    }

    public static String stringifyThrowable(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable);
        for(StackTraceElement elem : throwable.getStackTrace()) {
            sb.append("\n    at ").append(elem);
        }
        return sb.toString();
    }

    public static int generateRandomInt(int min, int max) {
        if(min > max) {
            throw new IllegalArgumentException("Min number cannot be larger than the max number.");
        }

        SecureRandom rand = new SecureRandom();
        return rand.nextInt(max - min + 1) + min;
    }

    public static String generateRandomHex(int byteLength) {
        if(byteLength <= 0) {
            throw new IllegalArgumentException("The byte length should be larger than zero.");
        }

        SecureRandom rand = new SecureRandom();
        byte[] randBytes = new byte[byteLength];
        rand.nextBytes(randBytes);

        StringBuilder sb = new StringBuilder();
        for(byte b : randBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String generateRandomCharSequence(int length, boolean specialChars) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"+ (specialChars ? "@$" : "");
        StringBuilder result = new StringBuilder();
        while(result.length() < length) {
            int charIndex = generateRandomInt(0, chars.length() - 1);
            result.append(chars.charAt(charIndex));
        }
        return result.toString();
    }

    public static boolean isNumeric(String str) {
        if(str == null || str.isEmpty()) return false;

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean validateLocaleCode(String localeCode) {
        if(localeCode == null || localeCode.isEmpty()) {
            return false;
        }

        final String standardCode = localeCode.toLowerCase().replaceAll("_", "-");
        Locale locale = Locale.forLanguageTag(standardCode);
        String language = locale.getLanguage();
        return !language.isEmpty() && !language.equals("und");
    }

    /**
     * @see <a href="https://stackoverflow.com/questions/5284147/validating-ipv4-addresses-with-regexp">https://stackoverflow.com/questions/5284147/validating-ipv4-addresses-with-regexp</a>
     */
    public static boolean validateIpv4Address(String ip) {
        final Pattern regex = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
        return regex.matcher(ip).matches();
    }

    public static int[] getImageDimensions(byte[] imageBytes) throws IOException {
        if(imageBytes == null || imageBytes.length == 0) throw new IllegalArgumentException("Empty image bytes");

        try(ByteArrayInputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            if(image == null) throw new IllegalArgumentException("Illegal image bytes");

            return new int[] { image.getWidth(), image.getHeight() };
        }
    }
}
