package sangiorgi.wps.opensource.algorithm

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Sealed class representing the result of a WPS PIN generation algorithm.
 * Using sealed class provides exhaustive pattern matching and type safety.
 *
 * Implements Parcelable for passing between Android components.
 */
sealed class AlgorithmResult : Parcelable {

    /**
     * Successful PIN generation result.
     *
     * @property pin The generated WPS PIN
     * @property algorithmName Name of the algorithm that generated the PIN
     */
    @Parcelize
    data class Success(
        val pin: String,
        val algorithmName: String,
    ) : AlgorithmResult()

    /**
     * Failed PIN generation result.
     *
     * @property errorMessage Description of what went wrong
     */
    @Parcelize
    data class Failure(
        val errorMessage: String,
    ) : AlgorithmResult()

    /**
     * Check if this result is successful.
     */
    val isSuccess: Boolean
        get() = this is Success

    companion object {

        /**
         * Create a successful result.
         */
        @JvmStatic
        fun success(pin: String, algorithmName: String): AlgorithmResult = Success(pin, algorithmName)

        /**
         * Create a failure result.
         */
        @JvmStatic
        fun failure(errorMessage: String): AlgorithmResult = Failure(errorMessage)
    }
}
