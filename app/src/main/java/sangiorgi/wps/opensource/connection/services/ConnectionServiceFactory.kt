package sangiorgi.wps.opensource.connection.services

import sangiorgi.wps.opensource.connection.ConnectionUpdateCallback
import sangiorgi.wps.opensource.connection.models.NetworkToTest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating ConnectionService instances.
 * This is a wrapper around the Hilt-generated ConnectionService.Factory
 * to maintain backward compatibility.
 *
 * @see ConnectionService.Factory - The actual Hilt-generated factory
 */
@Singleton
class ConnectionServiceFactory @Inject constructor(
    private val connectionServiceHiltFactory: ConnectionService.Factory,
) {
    fun create(networkToTest: NetworkToTest, callback: ConnectionUpdateCallback): ConnectionService {
        return connectionServiceHiltFactory.create(networkToTest, callback)
    }
}
