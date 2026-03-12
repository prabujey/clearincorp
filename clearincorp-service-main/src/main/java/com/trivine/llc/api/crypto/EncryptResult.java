package com.trivine.llc.api.crypto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EncryptResult {
    @JsonProperty("ciphertextB64")
    private String ciphertextB64;

    @JsonProperty("ivB64")
    private String ivB64;

    @JsonProperty("encryptedDekB64")
    private String encryptedDekB64;
}
