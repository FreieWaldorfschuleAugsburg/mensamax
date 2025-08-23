package de.waldorfaugsburg.mensamax.client.api;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode()
@ToString
public final class ApiError {

    private String code;
    private String message;
}
