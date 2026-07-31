package io.miragon.blueprint.domain.leasing

@JvmInline
value class Email(val value: String) {
    init {
        require(EMAIL_REGEX.matches(value)) { "'$value' is not a valid email address" }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }
}
