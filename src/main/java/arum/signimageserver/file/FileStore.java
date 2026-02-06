package arum.signimageserver.file;

import arum.signimageserver.model.FileItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
public record FileStore(
        @Value("${upload.directory}")
        String fileDirectory
) {

    public String getFullPath(String filename) {
        return fileDirectory + filename;
    }

    public void storeFile(
            MultipartFile multipartFile,
            String customFileName
    ) throws IOException {
        if (multipartFile.isEmpty()) {
            return;
        }

        var storeFileName = createStoreFileName(multipartFile, customFileName);

        log.info("storeFileName = {}", storeFileName);

        validateDirectory(fileDirectory);

        validateFile(storeFileName);

        multipartFile.transferTo(new File(getFullPath(storeFileName)));
    }

    private String createStoreFileName(
            MultipartFile multipartFile,
            String customFileName
    ) {
        if (hasText(customFileName)) {
            String ext = extractExt(multipartFile.getOriginalFilename());
            return customFileName + "." + ext;
        }

        return multipartFile.getOriginalFilename();
    }

    private String extractExt(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("originalFilename is null");
        }

        int pos = originalFilename.lastIndexOf('.');
        return originalFilename.substring(pos + 1);
    }

    private void validateFile(String storeFileName) throws IOException {
        try (Stream<Path> pathStream = Files.list(Path.of(fileDirectory))) {
            pathStream.filter(path -> getBaseName(path.getFileName().toString())
                    .equals(getBaseName(storeFileName))
            ).forEach(path -> {
                try {
                    Files.delete(path);
                    log.info("success delete exist userId file : {}", path.getFileName());
                } catch (IOException e) {
                    log.error("fail delete exist userId file : {}", path.getFileName(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public List<FileItem> findFiles() throws IOException {
        try (Stream<Path> pathStream = Files.list(Path.of(fileDirectory))) {
            return pathStream.map(p -> {
                        try {
                            return FileItem.from(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }
    }

    public void delete(String fileName) {
        String fullPath = getFullPath(fileName);

        try {
            Files.delete(Path.of(fullPath));
        } catch (IOException e) {
            log.error("fail delete file : {}", fullPath, e);
            throw new RuntimeException(e);
        }
    }

    public static boolean isImage(Path path) {
        try {
            var contentType = Files.probeContentType(path);

            return contentType != null && contentType.startsWith("image");
        } catch (IOException e) {
            return false;
        }
    }

    private void validateDirectory(String path) {
        Path target = Path.of(path);

        boolean exists = Files.exists(target);

        if (!exists) {
            try {
                Files.createDirectory(target);
            } catch (IOException e) {
                log.error("fail create directory : {}", target.toAbsolutePath());
            }
        }
    }
}
