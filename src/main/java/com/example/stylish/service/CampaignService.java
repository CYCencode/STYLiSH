package com.example.stylish.service;

import com.example.stylish.dao.CampaignDao;
import com.example.stylish.dto.CampaignRequest;
import com.example.stylish.dto.CampaignResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CampaignService {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignDao campaignDao;
    private final RedisTemplate<String, Object> redisTemplate;
    private final S3Service s3Service;
    public static final String CAMPAIGN_CACHE_KEY = "campaign_data";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CampaignService(CampaignDao campaignDao, RedisTemplate<String, Object> redisTemplate, S3Service s3Service) {
        this.campaignDao = campaignDao;
        this.redisTemplate = redisTemplate;
        this.s3Service = s3Service;
    }

    // 取得活動資訊
    public List<CampaignResponse> getCampaign() {
        try {
            // 1. 從 Redis 取得緩存
            String cachedData = (String) redisTemplate.opsForValue().get(CAMPAIGN_CACHE_KEY);
            if (cachedData != null) {
                log.info("cache hit : 使用緩存.");
                return objectMapper.readValue(cachedData,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, CampaignResponse.class));
            }

            // 2. 若緩存不存在，從資料庫讀取
            log.info("cache missed : 使用資料庫.");
            List<CampaignResponse> campaigns = campaignDao.selectCampaign();
            campaigns.forEach(campaign -> {
                // 3. 設定 S3 圖片 URL
                String imageUrl = s3Service.getFileUrl(campaign.getImageUrl());
                campaign.setImageUrl(imageUrl);
            });

            // 4. 將資料存入緩存
            String serializedData = objectMapper.writeValueAsString(campaigns);
            redisTemplate.opsForValue().set(CAMPAIGN_CACHE_KEY, serializedData);
            return campaigns;
        } catch (Exception e) {
            log.info("cache dead : 使用資料庫.");
            List<CampaignResponse> campaigns = campaignDao.selectCampaign();
            campaigns.forEach(campaign -> {
                String imageUrl = s3Service.getFileUrl(campaign.getImageUrl());
                campaign.setImageUrl(imageUrl);
            });
            return campaigns;
        }
    }

    // 儲存活動資訊
    public void saveCampaign(CampaignRequest campaignRequest) {
        try {
            // 1. 將圖片上傳到 S3
            String fileName = s3Service.uploadFile(campaignRequest.getImage());
            campaignDao.insertCampaign(campaignRequest.getProductId(), fileName, campaignRequest.getStory());

            try {
                // 2. 清除舊的緩存
                redisTemplate.delete(CAMPAIGN_CACHE_KEY);
                log.info("清除緩存 : 活動上傳成功");
            } catch (RedisConnectionFailureException e) {
                log.warn("刪除活動緩存失敗");
            }
        } catch (IOException e) {
            log.error("儲存活動圖片錯誤: " + e.getMessage(), e);
            throw new RuntimeException("儲存活動圖片錯誤", e);
        }
    }
}

