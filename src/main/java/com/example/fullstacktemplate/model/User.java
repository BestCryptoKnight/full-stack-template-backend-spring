package com.example.fullstacktemplate.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String requestedNewEmail;

    @OneToOne(targetEntity = FileDb.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_image")
    private FileDb profileImage;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String providerId;
    private String twoFactorSecret;

    @Column(nullable = false)
    private Boolean twoFactorEnabled;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<TwoFactorRecoveryCode> twoFactorRecoveryCodes;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<JwtToken> jwtTokens;
}
