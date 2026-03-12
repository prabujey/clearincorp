package com.trivine.llc.api.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {
    Admin(1L),
    Consumer(2L),
    SuperFiler(3L),
    Vendor(4L);

    final Long roleId;
}
