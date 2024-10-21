package com.example.stylish.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloudfront.domain}")
    private String cloudFrontDomain;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 上傳文件到 S3 並設置 Content-Type
    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", file.getContentType()); // 設置 Content-Type 為圖片的 MIME 類型

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .metadata(metadata)
                .contentType(file.getContentType()) // 設置 Content-Type
                .build();

        S3Response response = s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
        return fileName;
    }

    // 使用 CloudFront 來取得文件的 URL
    public String getFileUrl(String fileName) {
        // 使用 CloudFront 的域名構建圖片 URL
        return cloudFrontDomain + "/" + fileName;
    }
}
