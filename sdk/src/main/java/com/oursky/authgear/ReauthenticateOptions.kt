package com.oursky.authgear

import com.oursky.authgear.oauth.OIDCAuthenticationRequest

/**
 * Reauthentication options.
 */
data class ReauthentcateOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    var redirectUri: String,
    /**
     * OAuth 2.0 state value.
     */
    var state: String? = null,
    /**
     * UI locale tags
     */
    var uiLocales: List<String>? = null,

    /**
     * OIDC max_age.
     * The default is 0 if not provided.
     */
    var maxAge: Int? = null,

    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The wechatRedirectURI will be called when user click the login with WeChat button
     */
    var wechatRedirectURI: String? = null
)

internal fun ReauthentcateOptions.toRequest(idTokenHint: String, suppressIDPSessionCookie: Boolean): OIDCAuthenticationRequest {
    return OIDCAuthenticationRequest(
        redirectUri = this.redirectUri,
        responseType = "code",
        scope = listOf("openid", "https://authgear.com/scopes/full-access"),
        state = this.state,
        prompt = null,
        loginHint = null,
        idTokenHint = idTokenHint,
        maxAge = this.maxAge ?: 0,
        uiLocales = this.uiLocales,
        wechatRedirectURI = this.wechatRedirectURI,
        page = null,
        suppressIDPSessionCookie = suppressIDPSessionCookie
    )
}
