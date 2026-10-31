package com.NetflixClone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name="videos")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String title;
    @Column(length = 4000)
    private String description;

    private Integer year;
    private String rating;
    private Integer duration;

    @Column(name = "src")
    @JsonIgnore
    private String srcUuid;

    @Column(name = "poster")
    @JsonIgnore
    private String posterUuid;

    @Column(nullable = false)
    private boolean published=false;

    @Column(name = "category")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "video_categories",joinColumns = @JoinColumn(name = "video_id"))
    private List<String> categories=new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Transient
    @JsonProperty("isInWatchList")
    private boolean isInWatchList;
    @JsonProperty("src")
    public String getSrc(){
        if(srcUuid != null && !srcUuid.isEmpty()){
            // Relativ: der Vite-Proxy (und spaeter nginx) leitet /api an das Backend weiter
            return "/api/files/video/"+srcUuid;
        }
        return null;
    }

    @JsonProperty("poster")
    public String getPoster() {
        if(posterUuid !=null && !posterUuid.isEmpty()){
            return "/api/files/image/"+posterUuid;
        }
        return null;
    }

}
