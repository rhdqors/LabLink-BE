package com.example.lablink.domain.user.entity;

import com.example.lablink.global.timestamp.entity.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Getter
@Where(clause = "deleted_at IS NULL")
@SQLDelete(sql = "UPDATE terms SET deleted_at = CONVERT_TZ(now(), 'UTC', 'Asia/Seoul') WHERE id = ?")
@NoArgsConstructor
public class Terms extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean ageCheck;

    @Column(nullable = false)
    private boolean termsOfServiceAgreement;

    @Column(nullable = false)
    private boolean privacyPolicyConsent;

    @Column(nullable = false)
    private boolean sensitiveInfoConsent;

    @Column(nullable = false)
    private boolean marketingOptIn;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Terms(boolean ageCheck, boolean termsOfServiceAgreement,
                 boolean privacyPolicyConsent, boolean sensitiveInfoConsent,
                 boolean marketingOptIn, User user) {
        this.ageCheck = ageCheck;
        this.termsOfServiceAgreement = termsOfServiceAgreement;
        this.privacyPolicyConsent = privacyPolicyConsent;
        this.sensitiveInfoConsent = sensitiveInfoConsent;
        this.marketingOptIn = marketingOptIn;
        this.user = user;
    }
}
