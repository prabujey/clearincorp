package com.trivine.llc.api.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMasterDto {

    private Long documentTypeId;

    private String typeName;
}
