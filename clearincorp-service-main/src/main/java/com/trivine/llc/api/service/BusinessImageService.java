package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.BusinessImageDto;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BusinessImageService {

    private final S3Service s3;

    private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile("image_(\\d+)(\\.[A-Za-z0-9]+)?$");

    /* =========================
       Key helpers
       ========================= */

    private String buildKey(Long businessId, int index, String originalName) {
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.')); // keep .jpg/.png
        }
        return "business/%d/images/image_%d%s".formatted(businessId, index, ext);
    }

    private int extractId(String key) {
        String name = key.substring(key.lastIndexOf('/') + 1);
        Matcher m = IMAGE_NAME_PATTERN.matcher(name);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    private int getNextImageIndex(Long businessId) {
        String prefix = "business/%d/images/".formatted(businessId);
        List<S3Object> objs = s3.listByPrefix(prefix);

        return objs.stream()
                .map(S3Object::key)
                .map(k -> k.substring(k.lastIndexOf('/') + 1))
                .map(IMAGE_NAME_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(m -> Integer.parseInt(m.group(1)))
                .max()
                .orElse(0) + 1;
    }

    /* =========================
       Uploads
       ========================= */

    public List<String> uploadManyAtomic(Long businessId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        int next = getNextImageIndex(businessId);
        List<String> keys = new ArrayList<>(files.size());

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            String key = buildKey(businessId, next++, f.getOriginalFilename());
            s3.upload(key, f.getBytes(), f.getContentType());
            keys.add(key);
        }
        return keys;
    }

    public String uploadBusinessImage(Long businessId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        int nextIndex = getNextImageIndex(businessId);
        String key = buildKey(businessId, nextIndex, file.getOriginalFilename());
        s3.upload(key, file.getBytes(), file.getContentType());
        return key;
    }

    public List<String> uploadBusinessImages(Long businessId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        List<String> keys = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            keys.add(uploadBusinessImage(businessId, f));
        }
        return keys;
    }

    /* =========================
       LIST (metadata only — NO presigned URLs)
       ========================= */

    public List<BusinessImageDto> listImages(Long businessId) {
        final String prefix = "business/%d/images/".formatted(businessId);

        return s3.listByPrefix(prefix).stream()
                .sorted(Comparator.comparingInt(o -> extractId(o.key())))
                .map(this::toDto)
                .toList();
    }

    private BusinessImageDto toDto(S3Object o) {
        int id = extractId(o.key());
        String lastModifiedIso = (o.lastModified() == null)
                ? null
                : o.lastModified().atOffset(ZoneOffset.UTC).toInstant().toString(); // ISO-8601
        long size = (o.size() == null) ? 0L : o.size();

        return new BusinessImageDto(
                id,
                o.key(),
                size,
                lastModifiedIso,
                o.eTag()
        );
    }

    /* =========================
       View / Download
       ========================= */

    public ResponseEntity<byte[]> downloadBusinessImage(Long businessId, int imageId) {
        final String prefix = "business/%d/images/".formatted(businessId);

        String key = s3.listByPrefix(prefix).stream()
                .map(S3Object::key)
                .filter(k -> k.startsWith(prefix))
                .filter(k -> extractId(k) == imageId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        ResponseBytes<GetObjectResponse> bytes = s3.download(key);
        String contentType = bytes.response().contentType();
        String fileName = key.substring(key.lastIndexOf('/') + 1);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(bytes.asByteArray());
    }

    /* =========================
       Delete
       ========================= */

    public void deleteBusinessImage(Long businessId, int imageId) {
        final String prefix = "business/%d/images/".formatted(businessId);

        String key = s3.listByPrefix(prefix).stream()
                .map(S3Object::key)
                .filter(k -> k.startsWith(prefix))
                .filter(k -> extractId(k) == imageId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        s3.delete(key);
    }

    public int deleteAllBusinessImages(Long businessId) {
        String prefix = "business/%d/images/".formatted(businessId);
        var objs = s3.listByPrefix(prefix);
        List<String> keys = objs.stream().map(S3Object::key).toList();
        s3.deleteAllKeys(keys);
        return keys.size();
    }

    public String getBucketName() {
        return s3.getMarketplaceBucketName();
    }
}