package arum.signimageserver.model.request;

import org.springframework.web.multipart.MultipartFile;

public record UploadRequest(
        MultipartFile file,
        String customFilename
) {
}
