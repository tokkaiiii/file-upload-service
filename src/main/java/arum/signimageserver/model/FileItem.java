package arum.signimageserver.model;

import arum.signimageserver.file.FileStore;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record FileItem(
        String id,
        String name,
        long size,
        boolean isImage,
        LocalDateTime uploadDate
) {
    public static FileItem from(Path path) throws IOException {
        return new FileItem(
                FilenameUtils.getBaseName(path.getFileName().toString()),
                path.getFileName().toString(),
                Files.size(path) / 1024,
                FileStore.isImage(path),
                Files.getLastModifiedTime(path)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
        );
    }
}
