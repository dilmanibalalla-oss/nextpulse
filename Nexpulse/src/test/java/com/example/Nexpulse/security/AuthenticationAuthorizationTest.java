package com.example.Nexpulse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.example.Nexpulse.config.JwtProperties;
import com.example.Nexpulse.domain.CheckResult;
import com.example.Nexpulse.domain.Monitor;
import com.example.Nexpulse.dto.CreateMonitorRequest;
import com.example.Nexpulse.dto.UpdateMonitorRequest;
import com.example.Nexpulse.dto.UserLoginRequest;
import com.example.Nexpulse.dto.UserRegistrationRequest;
import com.example.Nexpulse.repository.AppUserRepository;
import com.example.Nexpulse.repository.CheckResultRepository;
import com.example.Nexpulse.repository.MonitorRepository;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AuthenticationAuthorizationTest {

	private static final String PASSWORD = "a-valid-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private AppUserRepository users;

	@Autowired
	private MonitorRepository monitors;

	@Autowired
	private CheckResultRepository checkResults;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtProperties jwtProperties;

	@Test
	void registerStoresABcryptHashAndHidesIt() throws Exception {
		String email = email();
		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UserRegistrationRequest("Ada Lovelace", email, PASSWORD))))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
		var stored = users.findByEmail(email.toLowerCase()).orElseThrow();

		assertThat(stored.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
		assertThat(body.path("email").asString()).isEqualTo(email.toLowerCase());
		assertThat(body.has("password")).isFalse();
		assertThat(body.has("passwordHash")).isFalse();
		assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD, stored.getPasswordHash());
	}

	@Test
	void duplicateEmailIsRejectedAndLoginAcceptsAnyCase(CapturedOutput output) throws Exception {
		String email = email();
		register("Ada", email, PASSWORD);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UserRegistrationRequest("Ada Again", email.toUpperCase(), PASSWORD))))
				.andExpect(status().isConflict());

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UserLoginRequest(email.toUpperCase(), PASSWORD))))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
				.andReturn();
		JsonNode body = jsonMapper.readTree(login.getResponse().getContentAsString());
		Instant expiresAt = Instant.parse(body.path("expiresAt").asString());

		assertThat(body.path("token").asString()).isNotBlank();
		assertThat(body.path("user").has("passwordHash")).isFalse();
		assertThat(expiresAt).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
		assertThat(expiresAt).isBefore(Instant.now().plus(Duration.ofMinutes(16)));
		assertThat(output.getAll()).doesNotContain(PASSWORD, body.path("token").asString());
	}

	@Test
	void unknownUserAndWrongPasswordLookTheSame(CapturedOutput output) throws Exception {
		String email = email();
		register("Ada", email, PASSWORD);
		String wrongPassword = "wrong-password";

		String unknown = loginBody(email(), wrongPassword);
		String mismatch = loginBody(email, wrongPassword);

		assertThat(jsonMapper.readTree(unknown).path("message").asString())
				.isEqualTo(jsonMapper.readTree(mismatch).path("message").asString())
				.isEqualTo("Invalid email or password");
		assertThat(unknown).doesNotContain(wrongPassword, "passwordHash");
		assertThat(mismatch).doesNotContain(wrongPassword, "passwordHash");
		assertThat(output.getAll()).doesNotContain(wrongPassword, PASSWORD);
	}

	@Test
	void protectedRoutesRequireAValidBearerToken() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/api/monitors"))
				.andExpect(status().isUnauthorized());

		String bogus = "not-a-jwt";
		MvcResult rejected = mockMvc.perform(get("/api/monitors")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + bogus))
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertThat(rejected.getResponse().getContentAsString())
				.contains("Invalid token")
				.doesNotContain(bogus);

		Account ada = registerAndLogin("Ada", email());
		String expired = expiredToken(ada.userId());
		MvcResult expiredResult = mockMvc.perform(get("/api/monitors")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertThat(expiredResult.getResponse().getContentAsString()).doesNotContain(expired);

		String tampered = tamper(ada.token());
		mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usersCanOnlyAccessTheirOwnMonitorsAndHistory() throws Exception {
		Account ada = registerAndLogin("Ada", email());
		Account grace = registerAndLogin("Grace", email());
		long monitorId = createMonitor(ada, "Ada API");

		mockMvc.perform(get("/api/monitors/" + monitorId).header(HttpHeaders.AUTHORIZATION, bearer(grace)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/monitors/" + monitorId + "/checks").header(HttpHeaders.AUTHORIZATION, bearer(grace)))
				.andExpect(status().isNotFound());
		mockMvc.perform(put("/api/monitors/" + monitorId)
						.header(HttpHeaders.AUTHORIZATION, bearer(grace))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UpdateMonitorRequest("Stolen", "https://example.com/stolen", 60, true))))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/monitors/" + monitorId).header(HttpHeaders.AUTHORIZATION, bearer(grace)))
				.andExpect(status().isNotFound());

		String graceList = mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, bearer(grace)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(graceList).doesNotContain("Ada API");

		Monitor monitor = monitors.findById(monitorId).orElseThrow();
		checkResults.saveAndFlush(new CheckResult(
				monitor,
				Instant.parse("2026-11-11T12:00:00Z"),
				false,
				503,
				1500L,
				"unavailable"
		));

		String history = mockMvc.perform(get("/api/monitors/" + monitorId + "/checks")
						.header(HttpHeaders.AUTHORIZATION, bearer(ada)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(history).contains("unavailable", "503");

		mockMvc.perform(put("/api/monitors/" + monitorId)
						.header(HttpHeaders.AUTHORIZATION, bearer(ada))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UpdateMonitorRequest("Renamed", "https://example.com/renamed", 30, false))))
				.andExpect(status().isOk());
		assertThat(monitors.findById(monitorId).orElseThrow().getName()).isEqualTo("Renamed");
		assertThat(monitors.findById(monitorId).orElseThrow().getOwner().getId()).isEqualTo(ada.userId());

		mockMvc.perform(delete("/api/monitors/" + monitorId).header(HttpHeaders.AUTHORIZATION, bearer(ada)))
				.andExpect(status().isNoContent());
		assertThat(monitors.findById(monitorId)).isEmpty();
		assertThat(checkResults.count()).isZero();
	}

	private Account registerAndLogin(String name, String email) throws Exception {
		register(name, email, PASSWORD);
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UserLoginRequest(email, PASSWORD))))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
		return new Account(body.path("token").asString(), body.path("user").path("id").asLong());
	}

	private void register(String name, String email, String password) throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UserRegistrationRequest(name, email, password))))
				.andExpect(status().isCreated());
	}

	private String loginBody(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new UserLoginRequest(email, password))))
				.andExpect(status().isUnauthorized())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private long createMonitor(Account owner, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/monitors")
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new CreateMonitorRequest(name, "https://example.com/health", 60, true))))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
		assertThat(body.path("ownerId").asLong()).isEqualTo(owner.userId());
		assertThat(body.has("passwordHash")).isFalse();
		return body.path("id").asLong();
	}

	private String expiredToken(long userId) throws Exception {
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
				new JWTClaimsSet.Builder()
						.issuer(JwtService.ISSUER)
						.subject(Long.toString(userId))
						.issueTime(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
						.expirationTime(Date.from(Instant.parse("2020-01-01T00:15:00Z")))
						.build()
		);
		jwt.sign(new MACSigner(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)));
		return jwt.serialize();
	}

	private static String tamper(String token) {
		char last = token.charAt(token.length() - 1);
		return token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');
	}

	private static String bearer(Account account) {
		return "Bearer " + account.token();
	}

	private String json(Object value) {
		return jsonMapper.writeValueAsString(value);
	}

	private static String email() {
		return "user-" + UUID.randomUUID() + "@nexpulse.test";
	}

	private record Account(String token, long userId) {
	}
}
