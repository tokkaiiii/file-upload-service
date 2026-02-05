package arum.signimageserver.controllers;

import arum.signimageserver.file.FileStore;
import arum.signimageserver.model.FileItem;
import arum.signimageserver.model.request.UploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@Slf4j
@Controller
public record SignController(
        FileStore fileStore
) {

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }

    @GetMapping("/files")
    public String files(
            Model model
    ) throws IOException {
        List<FileItem> fileItemList = fileStore.findFiles();

        model.addAttribute("files", fileItemList);

        return "files";
    }

    @PostMapping("/upload")
    public String upload(
            @ModelAttribute UploadRequest uploadRequest,
            RedirectAttributes redirectAttributes
    ) {
        log.info("==== start file upload ====");

        try {
            fileStore.storeFile(
                    uploadRequest.file(),
                    uploadRequest.customFilename()
            );
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "파일 업로드에 실패했습니다");

            return "redirect:/files";
        }

        log.info("==== end file upload ====");

        redirectAttributes.addFlashAttribute("message", "파일 업로드 성공!");

        return "redirect:/files";
    }

    @ResponseBody
    @GetMapping("/download/{userId}")
    public Resource downloadImage(@PathVariable String userId) throws IOException {
        FileItem item = fileStore.findFiles().stream()
                .filter(fileItem -> fileItem.id().equals(userId))
                .findAny()
                .orElseThrow();


        return new UrlResource("file:" + fileStore.getFullPath(item.name()));
    }

    @GetMapping("/attach/{userId}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable String userId) throws IOException {
        FileItem item = fileStore.findFiles().stream()
                .filter(fileItem -> fileItem.id().equals(userId))
                .findAny()
                .orElseThrow();

        UrlResource urlResource = new UrlResource("file:" + fileStore.getFullPath(item.name()));

        String encodedUploadFileName = UriUtils.encode(item.name(), UTF_8);
        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=\"" + encodedUploadFileName + "\"")
                .body(urlResource);
    }

    @PostMapping("/delete/{userId}")
    public String delete(
            @PathVariable String userId,
            RedirectAttributes redirectAttributes
    ) throws IOException {
        FileItem item = fileStore.findFiles().stream()
                .filter(fileItem -> fileItem.id().equals(userId))
                .findAny()
                .orElseThrow();

        try {
            fileStore.delete(item.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "파일 삭제에 실패했습니다");
        }

        redirectAttributes.addFlashAttribute("message", "파일을 삭제했습니다");

        return "redirect:/files";
    }
}
