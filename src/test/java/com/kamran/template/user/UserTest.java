package com.kamran.template.user;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void isAdmin_ShouldReturnTrue_WhenRoleIsAdmin() {
        User adminUser = User.builder().role(Role.ADMIN).build();
        User regularUser = User.builder().role(Role.USER).build();

        assertThat(adminUser.isAdmin()).isTrue();
        assertThat(regularUser.isAdmin()).isFalse();
    }

    @Test
    void hasProOrHigher_ShouldReturnTrue_WhenTierIsProOrPremium() {
        User proUser = User.builder().subscriptionTier(SubscriptionTier.PRO).build();
        User premiumUser = User.builder().subscriptionTier(SubscriptionTier.PREMIUM).build();
        User freeUser = User.builder().subscriptionTier(SubscriptionTier.FREE).build();

        assertThat(proUser.hasProOrHigher()).isTrue();
        assertThat(premiumUser.hasProOrHigher()).isTrue();
        assertThat(freeUser.hasProOrHigher()).isFalse();
    }

    @Test
    void hasPremium_ShouldReturnTrue_OnlyWhenTierIsPremium() {
        User premiumUser = User.builder().subscriptionTier(SubscriptionTier.PREMIUM).build();
        User proUser = User.builder().subscriptionTier(SubscriptionTier.PRO).build();
        User freeUser = User.builder().subscriptionTier(SubscriptionTier.FREE).build();

        assertThat(premiumUser.hasPremium()).isTrue();
        assertThat(proUser.hasPremium()).isFalse();
        assertThat(freeUser.hasPremium()).isFalse();
    }

    @Test
    void builder_ShouldCreateUserWithDefaultValues() {
        User user = User.builder()
            .email("test@example.com")
            .password("password")
            .firstName("John")
            .lastName("Doe")
            .build();

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.USER); // Default value
        assertThat(user.getSubscriptionTier()).isEqualTo(SubscriptionTier.FREE); // Default value
        assertThat(user.getEmailVerified()).isFalse(); // Default value
        assertThat(user.getAccountLocked()).isFalse(); // Default value
        assertThat(user.getEnabled()).isTrue(); // Default value
        assertThat(user.getMfaEnabled()).isFalse(); // Default value
    }

    @Test
    void equals_ShouldUseEmail_WhenComparingUsers() {
        User user1 = User.builder().email("test@example.com").build();
        User user2 = User.builder().email("test@example.com").build();
        User user3 = User.builder().email("different@example.com").build();

        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
    }

    @Test
    void hashCode_ShouldBeBasedOnEmail() {
        User user1 = User.builder().email("test@example.com").build();
        User user2 = User.builder().email("test@example.com").build();
        User user3 = User.builder().email("different@example.com").build();

        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        assertThat(user1.hashCode()).isNotEqualTo(user3.hashCode());
    }
}
