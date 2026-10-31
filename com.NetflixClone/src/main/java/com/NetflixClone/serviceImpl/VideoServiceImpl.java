package com.NetflixClone.serviceImpl;

import com.NetflixClone.dao.UserRepository;
import com.NetflixClone.dao.VideoRepository;
import com.NetflixClone.dto.request.VideoRequest;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.dto.response.PageResponse;
import com.NetflixClone.dto.response.VideoResponse;
import com.NetflixClone.dto.response.VideoStatsResponse;
import com.NetflixClone.entity.User;
import com.NetflixClone.entity.Video;
import com.NetflixClone.exception.ResourceNotFoundException;
import com.NetflixClone.service.FileService;
import com.NetflixClone.service.VideoService;
import com.NetflixClone.util.PaginationUtils;
import com.NetflixClone.util.ServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ServiceUtils serviceUtils;
    @Autowired
    private FileService fileService;

    // ---------- Admin ----------

    @Override
    public MessageResponse createVideoByAdmin(VideoRequest videoRequest) {
        Video video = new Video();
        applyRequest(video, videoRequest);
        videoRepository.save(video);
        return new MessageResponse("Video created successfully!");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getAllAdminVideos(int page, int size, String search) {
        Pageable pageable = PaginationUtils.createPageRequest(page, size, "id");
        Page<Video> videos = isBlank(search)
                ? videoRepository.findAll(pageable)
                : videoRepository.searchVideos(search.trim(), pageable);
        return PaginationUtils.toPageResponse(videos, VideoResponse::fromEntity);
    }

    @Override
    public MessageResponse updateVideoByAdmin(Long id, VideoRequest videoRequest) {
        Video video = serviceUtils.getVideoByIdOrThrow(id);
        String oldSrc = video.getSrcUuid();
        String oldPoster = video.getPosterUuid();
        applyRequest(video, videoRequest);
        videoRepository.save(video);
        // Ersetzte Dateien nicht verwaist auf der Platte liegen lassen
        if (oldSrc != null && !oldSrc.equals(video.getSrcUuid())) fileService.deleteVideo(oldSrc);
        if (oldPoster != null && !oldPoster.equals(video.getPosterUuid())) fileService.deleteImage(oldPoster);
        return new MessageResponse("Video updated successfully!");
    }

    @Override
    @Transactional
    public MessageResponse deleteVideoByAdmin(Long id) {
        Video video = serviceUtils.getVideoByIdOrThrow(id);
        // Erst aus allen Merklisten entfernen (Join-Tabelle user_watchlist)
        for (User user : userRepository.findAll()) {
            if (user.getWatchlist().removeIf(v -> v.getId() == video.getId())) {
                userRepository.save(user);
            }
        }
        videoRepository.delete(video);
        fileService.deleteVideo(video.getSrcUuid());
        fileService.deleteImage(video.getPosterUuid());
        return new MessageResponse("Video deleted successfully!");
    }

    @Override
    public MessageResponse toggleVideoPublishStatusByAdmin(Long id, boolean published) {
        Video video = serviceUtils.getVideoByIdOrThrow(id);
        video.setPublished(published);
        videoRepository.save(video);
        return new MessageResponse(published ? "Video published successfully!" : "Video unpublished successfully!");
    }

    @Override
    public VideoStatsResponse getAdminStats() {
        return new VideoStatsResponse(videoRepository.count(), videoRepository.countByPublishedTrue(),
                videoRepository.getTotalDuration());
    }

    // ---------- Benutzer ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getPublishedVideos(int page, int size, String search, String email) {
        Pageable pageable = PaginationUtils.createPageRequest(page, size, "id");
        Page<Video> videos = isBlank(search)
                ? videoRepository.findByPublishedTrue(pageable)
                : videoRepository.searchPublishedVideos(search.trim(), pageable);
        Set<Long> watchlistIds = watchlistIds(email);
        return PaginationUtils.toPageResponse(videos, v -> toResponse(v, watchlistIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoResponse> getFeaturedVideos(String email) {
        Set<Long> watchlistIds = watchlistIds(email);
        return videoRepository.findRandomPublishedVideos(5).stream()
                .map(v -> toResponse(v, watchlistIds))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VideoResponse getPublishedVideo(Long id, String email) {
        Video video = serviceUtils.getVideoByIdOrThrow(id);
        if (!video.isPublished()) {
            throw new ResourceNotFoundException("Video not found with id :" + id);
        }
        return toResponse(video, watchlistIds(email));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> getWatchlist(String email, int page, int size, String search) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        List<Video> videos = user.getWatchlist().stream()
                .filter(Video::isPublished)
                .filter(v -> isBlank(search) || v.getTitle().toLowerCase().contains(search.trim().toLowerCase()))
                .sorted(Comparator.comparing(Video::getTitle))
                .collect(Collectors.toCollection(ArrayList::new));

        Pageable pageable = PaginationUtils.createPageRequest(page, size);
        int from = Math.min((int) pageable.getOffset(), videos.size());
        int to = Math.min(from + pageable.getPageSize(), videos.size());
        Page<Video> pageResult = new PageImpl<>(videos.subList(from, to), pageable, videos.size());
        return PaginationUtils.toPageResponse(pageResult, v -> {
            VideoResponse response = VideoResponse.fromEntity(v);
            response.setInWatchList(true);
            return response;
        });
    }

    @Override
    @Transactional
    public MessageResponse addToWatchlist(String email, Long videoId) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        Video video = serviceUtils.getVideoByIdOrThrow(videoId);
        user.addToWatchList(video);
        userRepository.save(user);
        return new MessageResponse("Video added to watchlist!");
    }

    @Override
    @Transactional
    public MessageResponse removeFromWatchlist(String email, Long videoId) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        user.getWatchlist().removeIf(v -> v.getId() == videoId);
        userRepository.save(user);
        return new MessageResponse("Video removed from watchlist!");
    }

    // ---------- Hilfsmethoden ----------

    private void applyRequest(Video video, VideoRequest request) {
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setYear(request.getYear());
        video.setRating(request.getRating());
        video.setDuration(request.getDuration());
        video.setSrcUuid(emptyToNull(request.getSrc()));
        video.setPosterUuid(emptyToNull(request.getPoster()));
        video.setPublished(request.isPublished());
        video.setCategories(request.getCategories() != null ? new ArrayList<>(request.getCategories()) : new ArrayList<>());
    }

    private Set<Long> watchlistIds(String email) {
        return serviceUtils.getUserByEmailOrThrow(email).getWatchlist().stream()
                .map(Video::getId)
                .collect(Collectors.toSet());
    }

    private VideoResponse toResponse(Video video, Set<Long> watchlistIds) {
        VideoResponse response = VideoResponse.fromEntity(video);
        response.setInWatchList(watchlistIds.contains(video.getId()));
        return response;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
