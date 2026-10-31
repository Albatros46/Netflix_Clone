package com.NetflixClone.controller;

import com.NetflixClone.dto.request.VideoRequest;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.dto.response.PageResponse;
import com.NetflixClone.dto.response.VideoResponse;
import com.NetflixClone.dto.response.VideoStatsResponse;
import com.NetflixClone.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    // ---------- Admin ----------

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> createVideo(@Valid @RequestBody VideoRequest videoRequest){
        return ResponseEntity.ok(videoService.createVideoByAdmin(videoRequest));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<VideoResponse>> getAllAdminVideos(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size,
                                                                         @RequestParam(required = false) String search){
        return ResponseEntity.ok(videoService.getAllAdminVideos(page,size,search));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateVideo(@PathVariable Long id, @Valid @RequestBody VideoRequest videoRequest){
        return ResponseEntity.ok(videoService.updateVideoByAdmin(id,videoRequest));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteVideo(@PathVariable Long id){
        return ResponseEntity.ok(videoService.deleteVideoByAdmin(id));
    }

    @PatchMapping("/admin/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> togglePublish(@PathVariable Long id, @RequestParam boolean value){
        return ResponseEntity.ok(videoService.toggleVideoPublishStatusByAdmin(id,value));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VideoStatsResponse> getStats(){
        return ResponseEntity.ok(videoService.getAdminStats());
    }

    // ---------- Benutzer ----------

    @GetMapping("/published")
    public ResponseEntity<PageResponse<VideoResponse>> getPublishedVideos(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          @RequestParam(required = false) String search,
                                                                          Authentication authentication){
        return ResponseEntity.ok(videoService.getPublishedVideos(page,size,search,authentication.getName()));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<VideoResponse>> getFeaturedVideos(Authentication authentication){
        return ResponseEntity.ok(videoService.getFeaturedVideos(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(videoService.getPublishedVideo(id,authentication.getName()));
    }

    // ---------- Merkliste ----------

    @GetMapping("/watchlist")
    public ResponseEntity<PageResponse<VideoResponse>> getWatchlist(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(required = false) String search,
                                                                    Authentication authentication){
        return ResponseEntity.ok(videoService.getWatchlist(authentication.getName(),page,size,search));
    }

    @PostMapping("/watchlist/{videoId}")
    public ResponseEntity<MessageResponse> addToWatchlist(@PathVariable Long videoId, Authentication authentication){
        return ResponseEntity.ok(videoService.addToWatchlist(authentication.getName(),videoId));
    }

    @DeleteMapping("/watchlist/{videoId}")
    public ResponseEntity<MessageResponse> removeFromWatchlist(@PathVariable Long videoId, Authentication authentication){
        return ResponseEntity.ok(videoService.removeFromWatchlist(authentication.getName(),videoId));
    }
}
