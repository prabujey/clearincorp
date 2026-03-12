package com.trivine.llc.api.service;

import com.trivine.llc.api.config.AwsConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class S3Service {

    /**
     * Maximum number of objects to process in listing operations to prevent memory issues.
     */
    private static final int MAX_OBJECTS_LIMIT = 1000;

    /**
     * Maximum number of keys to delete in a single batch operation.
     */
    private static final int MAX_DELETE_BATCH_SIZE = 1000;

    @Getter
    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Getter
    @Value("${aws.s3.marketplace-bucket:}")
    private String marketplaceBucketName;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3Service(AwsConfiguration awsProperties, @Value("${aws.s3.enabled:false}") boolean s3Enabled) {
        this.s3Enabled = s3Enabled;

        if (!s3Enabled || isPlaceholder(awsProperties.getAccessKeyId())) {
            log.warn("AWS S3 is disabled or credentials are placeholders - S3 operations will not work!");
            this.s3Client = null;
            this.s3Presigner = null;
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                awsProperties.getAccessKeyId(),
                awsProperties.getSecretAccessKey()
        );

        Region region = Region.of(awsProperties.getRegion());

        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        log.info("AWS S3 client initialized successfully");
    }

    private boolean isPlaceholder(String value) {
        return value == null || value.isBlank() ||
               value.startsWith("YOUR_") || value.equals("placeholder");
    }

    private void requireS3() {
        if (s3Client == null) {
            throw new IllegalStateException("AWS S3 is not configured. Please set AWS_S3_ENABLED=true and provide valid AWS credentials.");
        }
    }

    public ResponseBytes<GetObjectResponse> downloadFile(String key) throws IOException {
        requireS3();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObjectAsBytes(getObjectRequest);
    }

    public String generatePresignedGetUrl(String key) {
        requireS3();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        URL url = presigned.url();
        return url.toString();
    }

    public void uploadFile(String key, ByteArrayOutputStream file) throws IOException {
        requireS3();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.toByteArray()));
    }

    public void uploadFileWithContentType(String key, byte[] fileBytes, String contentType) {
        requireS3();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
    }

    public void deleteFile(String key) {
        requireS3();
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    public boolean doesObjectExist(String key) {
        if (s3Client == null) {
            return false;
        }
        try {
            s3Client.headObject(b -> b.bucket(bucketName).key(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            // 404 also means not found (some setups return S3Exception with status 404)
            if (e.statusCode() == 404) return false;
            throw e; // important: don't hide real errors like 403
        }
    }

    public Optional<String> generatePresignedGetUrlIfExists(String key) {
        if (s3Client == null || !doesObjectExist(key)) {
            return Optional.empty();
        }
        return Optional.of(generatePresignedGetUrl(key));
    }

    public ResponseBytes<GetObjectResponse> download(String key) {
        requireS3();
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(marketplaceBucketName)
                .key(key)
                .build();
        return s3Client.getObjectAsBytes(req);
    }

    public void upload(String key, byte[] bytes, String contentType) {
        requireS3();
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(marketplaceBucketName)
                .key(key);
        if (contentType != null && !contentType.isBlank()) {
            req.contentType(contentType);
        }
        s3Client.putObject(req.build(), RequestBody.fromBytes(bytes));
    }

    public void delete(String key) {
        requireS3();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(marketplaceBucketName)
                .key(key)
                .build());
    }

    /**
     * Lists objects by prefix with a default limit to prevent memory issues.
     * Uses {@link #MAX_OBJECTS_LIMIT} as the maximum number of objects to return.
     */
    public List<S3Object> listByPrefix(String prefix) {
        return listByPrefix(prefix, MAX_OBJECTS_LIMIT);
    }

    /**
     * Lists objects by prefix with a specified limit.
     *
     * @param prefix the S3 key prefix to filter objects
     * @param maxKeys maximum number of objects to return (capped at {@link #MAX_OBJECTS_LIMIT})
     * @return list of S3Object matching the prefix
     */
    public List<S3Object> listByPrefix(String prefix, int maxKeys) {
        requireS3();
        int safeMaxKeys = Math.min(maxKeys, MAX_OBJECTS_LIMIT);

        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(marketplaceBucketName)
                .prefix(prefix)
                .maxKeys(safeMaxKeys)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(req);
        return new ArrayList<>(response.contents());
    }

    /**
     * Deletes multiple keys in batches to prevent issues with large deletions.
     * Automatically handles batching if the list exceeds the maximum batch size.
     */
    public void deleteAllKeys(List<String> keys) {
        requireS3();
        if (keys == null || keys.isEmpty()) return;

        // Process in batches to avoid exceeding AWS limits
        for (int i = 0; i < keys.size(); i += MAX_DELETE_BATCH_SIZE) {
            int endIndex = Math.min(i + MAX_DELETE_BATCH_SIZE, keys.size());
            List<String> batch = keys.subList(i, endIndex);

            DeleteObjectsRequest req = DeleteObjectsRequest.builder()
                    .bucket(marketplaceBucketName)
                    .delete(Delete.builder()
                            .objects(batch.stream()
                                    .map(k -> ObjectIdentifier.builder().key(k).build())
                                    .toList())
                            .build())
                    .build();

            s3Client.deleteObjects(req);
            log.debug("Deleted batch of {} objects from S3", batch.size());
        }
    }

    /**
     * Gets presigned URLs for objects in a directory with a default limit.
     * Uses {@link #MAX_OBJECTS_LIMIT} as the maximum number of URLs to generate.
     *
     * @param prefix the S3 key prefix (directory path)
     * @return list of presigned URLs
     */
    public List<String> getPresignedUrlsForDirectory(String prefix) {
        return getPresignedUrlsForDirectory(prefix, MAX_OBJECTS_LIMIT);
    }

    /**
     * Gets presigned URLs for objects in a directory with a specified limit.
     *
     * @param prefix the S3 key prefix (directory path)
     * @param maxObjects maximum number of objects to process (capped at {@link #MAX_OBJECTS_LIMIT})
     * @return list of presigned URLs
     */
    public List<String> getPresignedUrlsForDirectory(String prefix, int maxObjects) {
        requireS3();
        int safeMaxObjects = Math.min(maxObjects, MAX_OBJECTS_LIMIT);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .maxKeys(safeMaxObjects)
                .build();

        List<String> urls = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = listRequest.toBuilder();
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            for (S3Object s3Object : response.contents()) {
                if (urls.size() >= safeMaxObjects) {
                    log.warn("Reached maximum object limit ({}) for prefix: {}", safeMaxObjects, prefix);
                    return urls;
                }
                urls.add(generatePresignedGetUrl(s3Object.key()));
            }

            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;

        } while (continuationToken != null && urls.size() < safeMaxObjects);

        return urls;
    }
}
