package net.opanel.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class Utils {
    /**
     * @see <a href="https://ithelp.ithome.com.tw/articles/10212717">https://ithelp.ithome.com.tw/articles/10212717</a>
     */
    public static String md5(String str) {
        if(str == null) return "";
        final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest();
            StringBuffer sb = new StringBuffer(bytes.length * 2);
            for(Byte b : bytes) {
                sb.append(hexDigits[(b >> 4) & 0x0f]);
                sb.append(hexDigits[b & 0x0f]);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bytesToBase64URL(byte[] bytes) {
        final String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:image/png;base64,"+ base64; // png by default
    }

    public static String readTextFile(Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(Stream<String> lines = Files.lines(filePath)) {
            lines.forEach(sb::append);
        }
        return sb.toString();
    }

    public static String decompressTextGzip(Path gzipPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(
                FileInputStream fis = new FileInputStream(gzipPath.toString());
                GZIPInputStream gis = new GZIPInputStream(fis);
                InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)
        ) {
            reader.lines().forEach(sb::append);
        }
        return sb.toString();
    }
}
