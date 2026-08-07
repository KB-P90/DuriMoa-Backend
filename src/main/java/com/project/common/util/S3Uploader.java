package com.project.common.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.project.common.config.S3Config;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3;
    private final S3Config s3Config;

    public String upload(byte[] data, String dirName) {
        String key = dirName + "/" + UUID.randomUUID();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);

        amazonS3.putObject(s3Config.getBucket(), key, new ByteArrayInputStream(data), metadata);

        return amazonS3.getUrl(s3Config.getBucket(), key).toString();
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
