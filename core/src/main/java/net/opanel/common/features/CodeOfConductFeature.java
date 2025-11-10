package net.opanel.common.features;

import net.opanel.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

public interface CodeOfConductFeature {
    Path codeOfConductPath = Paths.get("").resolve("codeofconduct");

    default HashMap<String, String> getCodeOfConducts() throws IOException {
        HashMap<String, String> list = new HashMap<>();
        if(!Files.exists(codeOfConductPath)) {
            Files.createDirectory(codeOfConductPath);
        }

        try(Stream<Path> stream = Files.list(codeOfConductPath)) {
            stream.filter(item -> (
                            !Files.isDirectory(item)
                                    && item.toString().endsWith(".txt")
                                    && Utils.validateLocaleCode(item.getFileName().toString().replace(".txt", ""))
                    ))
                    .forEach(item -> {
                        try {
                            final String lang = item.getFileName().toString().replace(".txt", "");
                            final String content = Utils.readTextFile(item);
                            list.put(lang, content);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        return list;
    }

    default void updateOrCreateCodeOfConduct(String lang, String content) throws IOException {
        Path filePath = codeOfConductPath.resolve(lang +".txt");
        if(!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
        Utils.writeTextFile(filePath, content);
    }

    default void removeCodeOfConduct(String lang) throws IOException {
        Path filePath = codeOfConductPath.resolve(lang +".txt");
        Files.deleteIfExists(filePath);
    }
}
