package de.waldorfaugsburg.mensamax.common;

import lombok.*;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "username")
@ToString
public final class MensaMaxUser {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String dateOfBirth;
    private String userGroup;
    private Set<String> chipIds;
}
