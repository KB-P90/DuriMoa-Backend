package com.project.common.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.project.common.config.S3Config;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Log4j2
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3;
    private final S3Config s3Config;

    public String upload(byte[] data, String dirName) {
        String key = dirName + "/" + UUID.randomUUID();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);

        log.info("S3 업로드 요청: bucket={}, region={}, key={}, size={} bytes",
                s3Config.getBucket(), s3Config.getRegion(), key, data.length);

        try {
            amazonS3.putObject(s3Config.getBucket(), key, new ByteArrayInputStream(data), metadata);
        } catch (AmazonServiceException e) {
            log.error("S3 업로드 실패 (AmazonServiceException): statusCode={}, errorCode={}, errorType={}, "
                            + "requestId={}, message={}",
                    e.getStatusCode(), e.getErrorCode(), e.getErrorType(), e.getRequestId(), e.getMessage(), e);
            throw e;
        } catch (SdkClientException e) {
            log.error("S3 업로드 실패 (SdkClientException, 자격증명/네트워크 문제 가능성): {}", e.getMessage(), e);
            throw e;
        }

        String url = amazonS3.getUrl(s3Config.getBucket(), key).toString();
        log.info("S3 업로드 성공: url={}", url);
        return url;
    }

    public void delete(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            String key = new URL(url).getPath().substring(1);
            amazonS3.deleteObject(s3Config.getBucket(), key);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("잘못된 S3 URL입니다: " + url, e);
        }
    }
}
