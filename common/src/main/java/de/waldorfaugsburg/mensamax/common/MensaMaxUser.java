package de.waldorfaugsburg.mensamax.common;

import lombok.*;

import java.util.List;
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
    private List<String> contactEmails;
    private String dateOfBirth;
    private String userGroup;
    private int employeeId;
}
