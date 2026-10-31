package com.NetflixClone.entity;

import com.NetflixClone.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false,unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String fullName;

    @Column(name = "user_name",nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role=Role.USER;

    @Column(nullable = false)
    private boolean active=true;
    @Column(nullable = false)
    private boolean emailVerified=false;
    @Column(unique = true)
    private String verificationToken;
    @Column
    private Instant verificationTokenExpiry;
    @Column
    private String passwordResetToken;
    @Column
    private Instant passwordResetTokenExpiry;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="user_watchlist",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name="video_id")
    )
    private Set<Video> watchlist=new HashSet<>();

    public void addToWatchList(Video video){
        this.watchlist.add(video);
    }
    public void removeToWatchList(Video video){
        this.watchlist.remove(video);
    }

}
