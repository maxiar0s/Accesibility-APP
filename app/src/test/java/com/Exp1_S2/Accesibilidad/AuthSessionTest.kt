package com.Exp1_S2.Accesibilidad

import com.Exp1_S2.Accesibilidad.auth.AuthGateway
import com.Exp1_S2.Accesibilidad.auth.AuthIdentity
import com.Exp1_S2.Accesibilidad.auth.AuthRoute
import com.Exp1_S2.Accesibilidad.auth.AuthSession
import com.Exp1_S2.Accesibilidad.auth.LocalProfileStore
import com.Exp1_S2.Accesibilidad.auth.PreferenceValues
import com.Exp1_S2.Accesibilidad.auth.RegisteredAccount
import org.junit.Assert.*
import org.junit.Test

/** Exercises orchestration and serialization, not Firebase or Android SharedPreferences runtime. */
class AuthSessionTest {
    private val identity = AuthIdentity("uid-ana", "ana@example.com", "Ana online")
    private val submitted = User(
        name = "Ana local", email = "ana@example.com",
        communicationPreference = CommunicationPreference.TEXT_MESSAGE,
        primaryCommunicationMode = CommunicationMode.VISUAL,
        accessibilityPreferences = setOf(AccessibilityPreference.VISUAL_ALERTS)
    )

    @Test
    fun login_waitsForSuccessAndPreventsDuplicateOperationsOrNavigation() {
        val fixture = Fixture()
        fixture.session.login(" ana@example.com ", "secret")
        assertTrue(fixture.session.state.busy)
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
        assertEquals("ana@example.com", fixture.auth.lastEmail)
        fixture.session.login("ana@example.com", "secret")
        fixture.session.resetPassword("ana@example.com")
        fixture.session.navigate(AuthRoute.REGISTRATION)
        assertEquals(1, fixture.auth.loginCalls)
        assertEquals(0, fixture.auth.resetCalls)
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)

        fixture.auth.completeLogin(Result.success(identity))
        assertEquals(AuthRoute.HOME, fixture.session.state.route)
        assertFalse(fixture.session.state.busy)
        assertEquals("uid-ana", fixture.values.get("current.uid"))
        assertEquals("Ana online", fixture.session.state.user?.name)
    }

    @Test
    fun login_failureRemainsOnLoginAndAllowsRetry() {
        val fixture = Fixture()
        fixture.session.login("ana@example.com", "wrong")
        fixture.auth.completeLogin(Result.failure(IllegalStateException("Check your details and try again.")))
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
        assertFalse(fixture.session.state.busy)
        assertNull(fixture.session.state.user)
        assertEquals("Check your details and try again.", fixture.session.state.message)
        assertNull(fixture.values.get("current.uid"))
        fixture.session.login("ana@example.com", "secret")
        assertEquals(2, fixture.auth.loginCalls)
        assertNull(fixture.session.state.message)
    }

    @Test
    fun invalidInputNeverStartsAuthentication() {
        val fixture = Fixture()
        fixture.session.login("not-an-email", "")
        fixture.session.register(submitted, "short")
        fixture.session.resetPassword("invalid")
        assertEquals(0, fixture.auth.loginCalls)
        assertEquals(0, fixture.auth.registerCalls)
        assertEquals(0, fixture.auth.resetCalls)
        assertFalse(fixture.session.state.busy)
        assertNotNull(fixture.session.state.message)
        fixture.session.navigate(AuthRoute.HOME)
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
    }

    @Test
    fun registration_successSavesUidScopedSelectionsAndReturnsToLoginSignedOut() {
        val fixture = Fixture()
        fixture.session.navigate(AuthRoute.REGISTRATION)
        fixture.session.register(submitted, "Secret1!")
        fixture.session.register(submitted, "Secret1!")
        assertTrue(fixture.session.state.busy)
        assertEquals(1, fixture.auth.registerCalls)
        fixture.auth.completeRegistration(Result.success(RegisteredAccount(identity)))
        assertEquals(submitted.copy(uid = identity.uid), fixture.store.profile(identity.uid))
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
        assertFalse(fixture.session.state.busy)
        assertNull(fixture.auth.currentIdentity)
        assertNull(fixture.values.get("current.uid"))
        assertEquals("Cuenta creada. Inicia sesión para continuar.", fixture.session.state.message)
    }

    @Test
    fun registration_profileWarningIsAccountSuccessWithLocalNameFallback() {
        val fixture = Fixture()
        fixture.session.navigate(AuthRoute.REGISTRATION)
        fixture.session.register(submitted, "Secret1!")
        fixture.auth.completeRegistration(Result.success(RegisteredAccount(identity.copy(name = null), true)))
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
        assertTrue(fixture.session.state.message!!.startsWith("Cuenta creada."))
        assertTrue(fixture.session.state.message!!.contains("no se pudo actualizar en línea"))
        assertEquals("Ana local", fixture.store.profile(identity.uid)?.name)
        fixture.session.login("ana@example.com", "secret")
        fixture.auth.completeLogin(Result.success(identity.copy(name = null)))
        assertEquals("Ana local", fixture.session.state.user?.name)
    }

    @Test
    fun registration_failureDoesNotSaveProfileOrClaimCreation() {
        val fixture = Fixture()
        fixture.session.navigate(AuthRoute.REGISTRATION)
        fixture.session.register(submitted, "Secret1!")
        fixture.auth.completeRegistration(Result.failure(IllegalStateException("Account could not be created.")))
        assertEquals(AuthRoute.REGISTRATION, fixture.session.state.route)
        assertFalse(fixture.session.state.busy)
        assertEquals("Account could not be created.", fixture.session.state.message)
        assertNull(fixture.store.profile(identity.uid))
    }

    @Test
    fun registration_storesSelectionsBeforeNameUpdateWithoutGrantingHome() {
        val fixture = Fixture()
        fixture.session.navigate(AuthRoute.REGISTRATION)
        fixture.session.register(submitted, "Secret1!")
        fixture.auth.created(identity)
        assertEquals(submitted.copy(uid = identity.uid), fixture.store.profile(identity.uid))
        assertEquals(AuthRoute.REGISTRATION, fixture.session.state.route)
        assertTrue(fixture.session.state.busy)
        assertNull(fixture.session.state.user)
        assertNull(fixture.values.get("current.uid"))
    }

    @Test
    fun restore_requiresIdentityAndClearsCachedCurrentMetadataWhenAbsent() {
        val values = MemoryValues()
        val store = LocalProfileStore(values)
        store.saveProfile(submitted.copy(uid = identity.uid))
        store.saveCurrent(submitted.copy(uid = identity.uid))
        val session = AuthSession(FakeAuth(), store)
        assertEquals(AuthRoute.LOGIN, session.state.route)
        assertNull(session.state.user)
        assertNull(values.get("current.uid"))
        assertNotNull(store.profile(identity.uid))
    }

    @Test
    fun restore_refreshesMetadataFromIdentityWithoutAnotherUsersCachedName() {
        val values = MemoryValues()
        val store = LocalProfileStore(values)
        store.saveProfile(submitted.copy(uid = "other-uid"))
        store.saveCurrent(submitted.copy(uid = "other-uid"))
        val session = AuthSession(FakeAuth(identity.copy(name = null)), store)
        assertEquals(AuthRoute.HOME, session.state.route)
        assertEquals("", session.state.user?.name)
        assertEquals(identity.uid, values.get("current.uid"))
        assertEquals(identity.email, values.get("current.email"))
        assertEquals("", values.get("current.name"))
    }

    @Test
    fun restore_keepsOwnSelectionsButRefreshesEmailAndOnlineName() {
        val values = MemoryValues()
        val store = LocalProfileStore(values)
        store.saveProfile(submitted.copy(uid = identity.uid, email = "old@example.com"))
        val session = AuthSession(FakeAuth(identity), store)
        assertEquals(identity.email, session.state.user?.email)
        assertEquals(identity.name, session.state.user?.name)
        assertEquals(submitted.accessibilityPreferences, session.state.user?.accessibilityPreferences)
        assertEquals(submitted.primaryCommunicationMode, session.state.user?.primaryCommunicationMode)
    }

    @Test
    fun logout_clearsIdentityAndCurrentMetadataButRetainsLocalPreferences() {
        val fixture = Fixture(identity)
        fixture.store.saveProfile(submitted.copy(uid = identity.uid))
        fixture.session.logout()
        assertEquals(1, fixture.auth.signOutCalls)
        assertNull(fixture.auth.currentIdentity)
        assertNull(fixture.values.get("current.uid"))
        assertNull(fixture.values.get("current.email"))
        assertNull(fixture.values.get("current.name"))
        assertNotNull(fixture.store.profile(identity.uid))
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
        fixture.session.navigate(AuthRoute.HOME)
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
    }

    @Test
    fun recovery_waitsForGenericSuccessAndPreventsDuplicates() {
        val fixture = Fixture()
        fixture.session.navigate(AuthRoute.RECOVERY)
        fixture.session.resetPassword(" ana@example.com ")
        fixture.session.resetPassword("ana@example.com")
        assertTrue(fixture.session.state.busy)
        assertEquals(1, fixture.auth.resetCalls)
        assertEquals("ana@example.com", fixture.auth.lastEmail)
        fixture.auth.resetDone(Result.success(Unit))
        assertFalse(fixture.session.state.busy)
        assertEquals(AuthRoute.RECOVERY, fixture.session.state.route)
        assertEquals("Si existe una cuenta con este correo, se enviarán instrucciones para recuperar la contraseña.", fixture.session.state.message)
    }

    @Test
    fun recovery_failureAllowsRetryWithoutSuccessClaim() {
        val fixture = Fixture()
        fixture.session.navigate(AuthRoute.RECOVERY)
        fixture.session.resetPassword("ana@example.com")
        fixture.auth.resetDone(Result.failure(IllegalStateException("Connection unavailable.")))
        assertFalse(fixture.session.state.busy)
        assertEquals("Connection unavailable.", fixture.session.state.message)
        fixture.session.resetPassword("ana@example.com")
        assertEquals(2, fixture.auth.resetCalls)
    }

    @Test
    fun disposedScopeIgnoresLateCallbacksAndCannotStartNewRequests() {
        val fixture = Fixture()
        var notifications = 0
        fixture.session.onChange = { notifications++ }
        fixture.session.login("ana@example.com", "secret")
        val before = fixture.session.state
        fixture.session.close()
        fixture.auth.completeLogin(Result.success(identity))
        fixture.session.resetPassword("ana@example.com")
        assertEquals(before, fixture.session.state)
        assertEquals(1, notifications)
        assertNull(fixture.values.get("current.uid"))
        assertEquals(0, fixture.auth.resetCalls)
    }

    @Test
    fun logoutInvalidatesPendingCallbackUiMutation() {
        val fixture = Fixture()
        fixture.session.login("ana@example.com", "secret")
        fixture.session.logout()
        fixture.auth.completeLogin(Result.success(identity))
        assertEquals(AuthRoute.LOGIN, fixture.session.state.route)
        assertNull(fixture.session.state.user)
        assertNull(fixture.values.get("current.uid"))
    }

    @Test
    fun profileSerializationIsUidScopedAndContainsOnlyPasswordFreeFields() {
        val fixture = Fixture()
        val profile = submitted.copy(uid = identity.uid)
        fixture.store.saveProfile(profile)
        fixture.store.saveCurrent(profile)
        assertEquals(profile, fixture.store.profile(identity.uid))
        assertNull(fixture.store.profile("other-uid"))
        assertEquals(setOf(
            "profile.uid-ana.name", "profile.uid-ana.email", "profile.uid-ana.communication",
            "profile.uid-ana.mode", "profile.uid-ana.accessibility",
            "current.uid", "current.email", "current.name"
        ), fixture.values.data.keys)
        fixture.values.write(mapOf("profile.uid-ana.mode" to "unknown", "profile.uid-ana.accessibility" to "unknown,VISUAL_ALERTS"))
        assertEquals(CommunicationMode.WRITTEN, fixture.store.profile(identity.uid)?.primaryCommunicationMode)
        assertEquals(setOf(AccessibilityPreference.VISUAL_ALERTS), fixture.store.profile(identity.uid)?.accessibilityPreferences)
    }

    @Test
    fun registration_invalidPolicyNeverCallsGateway() {
        val fixture = Fixture()
        fixture.session.register(submitted.copy(name = " Á "), "Secret1!")
        listOf("Aa1!aa", "aaaaaa1!", "AAAAAA1!", "Aaaaaaa!", "Aaaaaa1", "Aaaaa1 ").forEach {
            fixture.session.register(submitted, it)
            assertNotNull(fixture.session.state.message)
        }
        assertEquals(0, fixture.auth.registerCalls)
        assertFalse(fixture.session.state.busy)
    }

    @Test
    fun login_acceptsLegacyPasswordWithoutRegistrationComplexity() {
        val fixture = Fixture()
        fixture.session.login("ana@example.com", "old")
        assertEquals(1, fixture.auth.loginCalls)
        fixture.auth.completeLogin(Result.success(identity))
        assertEquals(AuthRoute.HOME, fixture.session.state.route)
    }

    private class Fixture(identity: AuthIdentity? = null) {
        val auth = FakeAuth(identity)
        val values = MemoryValues()
        val store = LocalProfileStore(values)
        val session = AuthSession(auth, store)
    }

    private class MemoryValues : PreferenceValues {
        val data = mutableMapOf<String, String>()
        override fun get(key: String) = data[key]
        override fun write(values: Map<String, String?>) {
            values.forEach { (key, value) -> if (value == null) data.remove(key) else data[key] = value }
        }
    }

    private class FakeAuth(override var currentIdentity: AuthIdentity? = null) : AuthGateway {
        var loginCalls = 0
        var registerCalls = 0
        var resetCalls = 0
        var signOutCalls = 0
        var lastEmail = ""
        private lateinit var loginDone: (Result<AuthIdentity>) -> Unit
        private lateinit var registerDone: (Result<RegisteredAccount>) -> Unit
        lateinit var created: (AuthIdentity) -> Unit
        lateinit var resetDone: (Result<Unit>) -> Unit
        override fun login(email: String, password: String, done: (Result<AuthIdentity>) -> Unit) {
            loginCalls++
            lastEmail = email
            loginDone = done
        }
        override fun register(
            name: String, email: String, password: String,
            onCreated: (AuthIdentity) -> Unit, done: (Result<RegisteredAccount>) -> Unit
        ) {
            registerCalls++
            lastEmail = email
            registerDone = done
            created = onCreated
        }
        override fun resetPassword(email: String, done: (Result<Unit>) -> Unit) {
            resetCalls++
            lastEmail = email
            resetDone = done
        }
        override fun signOut() {
            signOutCalls++
            currentIdentity = null
        }
        fun completeLogin(result: Result<AuthIdentity>) {
            currentIdentity = result.getOrNull()
            loginDone(result)
        }
        fun completeRegistration(result: Result<RegisteredAccount>) {
            result.getOrNull()?.let {
                created(it.identity)
                signOut()
            }
            registerDone(result)
        }
    }
}
