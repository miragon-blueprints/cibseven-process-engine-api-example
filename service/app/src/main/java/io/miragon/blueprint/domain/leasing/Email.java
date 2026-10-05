package io.miragon.blueprint.domain.leasing;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (!EMAIL_REGEX.matcher(value).matches()) {
            throw new IllegalArgumentException("'" + value + "' is not a valid email address");
        }
    }
}
