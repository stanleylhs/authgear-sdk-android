package com.oursky.authgear

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.oursky.authgear.data.key.KeyRepoKeystore
import com.oursky.authgear.data.oauth.OauthRepo
import com.oursky.authgear.oauth.*
import com.oursky.authgear.oauth.OIDCConfiguration
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class AuthgearTest {
    companion object {
        private const val RefreshTokenWaitMs = 1000L

        private val ClientId = Base64.getEncoder().encodeToString(Random.nextBytes(16))
    }
    // Currently the mock is test specific, if it gains sufficient generic features, can move it
    // back to data layer.
    private class OauthRepoMock : OauthRepo {
        private val refreshedCount = AtomicInteger(0)
        override var endpoint: String? = null

        fun getRefreshedCount(): Int {
            return refreshedCount.get()
        }

        override fun getOIDCConfiguration(): OIDCConfiguration {
            return OIDCConfiguration("/authorize", "/token", "/userInfo", "/revoke", "logout")
        }

        override fun oidcTokenRequest(request: OIDCTokenRequest): OIDCTokenResponse {
            if (request.grantType == GrantType.REFRESH_TOKEN) {
                refreshedCount.accumulateAndGet(1) { a, b ->
                    a + b
                }
            }
            Thread.sleep(RefreshTokenWaitMs)
            return OIDCTokenResponse("idToken", "code", "accessToken", 0, "refreshToken")
        }

        override fun oidcRevocationRequest(refreshToken: String) {
        }

        override fun oidcUserInfoRequest(accessToken: String): UserInfo {
            return UserInfo("sub", isVerified = false, isAnonymous = false)
        }

        override fun oauthChallenge(purpose: String): ChallengeResponse {
            return ChallengeResponse("abc", "0")
        }

        override fun biometricSetupRequest(accessToken: String, clientId: String, jwt: String) {
            TODO("Not yet implemented")
        }

        override fun oauthAppSessionToken(refreshToken: String): AppSessionTokenResponse {
            TODO("Not yet implemented")
        }

        override fun wechatAuthCallback(code: String, state: String) {
            TODO("Not yet implemented")
        }
    }

    private class RefreshTokenRepoMock : TokenStorage {
        override fun setRefreshToken(namespace: String, refreshToken: String) {
        }

        override fun getRefreshToken(namespace: String): String? {
            return null
        }

        override fun deleteRefreshToken(namespace: String) {
        }
    }

    private class TokenRepoMock : ContainerStorage {
        override fun setOIDCCodeVerifier(namespace: String, verifier: String) {
        }

        override fun getOIDCCodeVerifier(namespace: String): String? {
            return null
        }

        override fun getAnonymousKeyId(namespace: String): String? {
            return null
        }

        override fun setAnonymousKeyId(namespace: String, keyId: String) {
        }

        override fun deleteAnonymousKeyId(namespace: String) {
        }

        override fun getBiometricKeyId(namespace: String): String? {
            return null
        }

        override fun setBiometricKeyId(namespace: String, keyId: String) {
        }

        override fun deleteBiometricKeyId(namespace: String) {
        }
    }

    private lateinit var oauthMock: OauthRepoMock
    private lateinit var authgearCore: AuthgearCore
    private lateinit var authgearNotUsed: Authgear

    @Before
    fun setup() {
        val application =
            InstrumentationRegistry.getInstrumentation().context.applicationContext as Application
        oauthMock = OauthRepoMock()
        authgearNotUsed = Authgear(
            application,
            ClientId,
            "EndpointNotUsed"
        )
        authgearCore = AuthgearCore(
            authgearNotUsed,
            application,
            ClientId,
            "EndpointNotUsed",
            false,
            RefreshTokenRepoMock(),
            TokenRepoMock(),
            oauthMock,
            KeyRepoKeystore()
        )
    }

    @Test
    fun configure() {
        assertEquals(
            "ClientId is not configured properly. Client id",
            ClientId,
            authgearCore.clientId
        )
    }

    @Test
    fun serializeGrantType() {
        val testOIDCTokenRequest = OIDCTokenRequest(
            grantType = GrantType.ANONYMOUS,
            clientId = "test",
            xDeviceInfo = "dummy"
        )
        val encoded = Json.encodeToString(
            testOIDCTokenRequest
        )
        assertEquals(
            "Encoded OIDCTokenRequest does not match with expected JSON string",
            encoded,
            "{\"grant_type\":\"urn:authgear:params:oauth:grant-type:anonymous-request\"," +
                    "\"client_id\":\"test\",\"x_device_info\":\"dummy\",\"redirect_uri\":null,\"code\":null,\"code_verifier\":null,\"refresh_token\":null,\"access_token\":null,\"jwt\":null}"
        )
        val decoded: OIDCTokenRequest = Json.decodeFromString(encoded)
        assertEquals(
            "Expect serializing and deserializing with recover the same object",
            decoded,
            testOIDCTokenRequest
        )
    }
}
