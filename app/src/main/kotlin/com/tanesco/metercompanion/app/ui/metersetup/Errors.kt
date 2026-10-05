package com.tanesco.metercompanion.app.ui.metersetup

import com.tanesco.metercompanion.core.model.DomainError

/**
 * Explains a non-field failure in the user's terms.
 *
 * Copy must never imply a meter was reached; none of these branches can be
 * produced by a successful meter operation.
 */
internal fun DomainError.userFacingMessage(): String = when (this) {
    is DomainError.Validation -> detail
    is DomainError.NotFound -> "That meter is no longer available."
    is DomainError.Conflict -> detail
    is DomainError.Storage -> "Local storage could not be updated. Free some space and try again."
    is DomainError.Connection -> detail
    is DomainError.Unavailable -> detail
    is DomainError.Unexpected -> "Something went wrong. The details are in Diagnostics."
}
