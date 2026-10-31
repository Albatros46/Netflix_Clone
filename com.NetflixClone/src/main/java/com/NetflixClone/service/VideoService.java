package com.NetflixClone.service;

import com.NetflixClone.dto.request.VideoRequest;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.dto.response.PageResponse;
import com.NetflixClone.dto.response.VideoResponse;
import com.NetflixClone.dto.response.VideoStatsResponse;

import java.util.List;

public interface VideoService {
    MessageResponse createVideoByAdmin(VideoRequest videoRequest);

    PageResponse<VideoResponse> getAllAdminVideos(int page, int size, String search);

    MessageResponse updateVideoByAdmin(Long id, VideoRequest videoRequest);

    MessageResponse deleteVideoByAdmin(Long id);

    MessageResponse toggleVideoPublishStatusByAdmin(Long id, boolean published);

    VideoStatsResponse getAdminStats();

    PageResponse<VideoResponse> getPublishedVideos(int page, int size, String search, String email);

    List<VideoResponse> getFeaturedVideos(String email);

    VideoResponse getPublishedVideo(Long id, String email);

    PageResponse<VideoResponse> getWatchlist(String email, int page, int size, String search);

    MessageResponse addToWatchlist(String email, Long videoId);

    MessageResponse removeFromWatchlist(String email, Long videoId);
}
