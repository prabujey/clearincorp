package com.trivine.llc.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyToggleResponseDto {
    /** true if business is now verified; false if now unverified */
    private boolean verified;

    /** "VERIFIED" | "UNVERIFIED" | "NO_CHANGE" */
    private String status;

    /** optional human-friendly note */
    private String message;
}
