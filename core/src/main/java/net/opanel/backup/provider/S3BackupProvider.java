package net.opanel.backup.provider;

import net.opanel.backup.BackupS3Config;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public class S3BackupProvider implements BackupProvider {
    private final BackupS3Config config;

    public S3BackupProvider(BackupS3Config config) {
        this.config = config;
    }

    @Override
    public void testConnection() {
        try(S3Client client = createClient()) {
            client.headBucket(HeadBucketRequest.builder().bucket(config.bucket).build());
        }
    }

    @Override
    public void upload(Path sourcePath, String remoteKey, Map<String, String> metadata) {
        try(S3Client client = createClient()) {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(remoteKey)
                    .metadata(metadata)
                    .build();
            client.putObject(req, RequestBody.fromFile(sourcePath));
        }
    }

    @Override
    public void download(String remoteKey, Path targetPath) {
        try(S3Client client = createClient()) {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(remoteKey)
                    .build();
            client.getObject(req, targetPath);
        }
    }

    @Override
    public void delete(String remoteKey) {
        try(S3Client client = createClient()) {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(remoteKey)
                    .build();
            client.deleteObject(req);
        }
    }

    private S3Client createClient() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(config.region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey, config.secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(config.forcePathStyle)
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(15))
                        .apiCallAttemptTimeout(Duration.ofMinutes(10))
                        .build());
        if(config.endpoint != null && !config.endpoint.isBlank()) {
            builder.endpointOverride(URI.create(config.endpoint));
        }
        return builder.build();
    }
}
