package io.vmware.spring.ai.azure.openai;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsUsage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.HttpClientOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.cp.elements.lang.ObjectUtils;
import org.cp.elements.lang.annotation.NotNull;

import lombok.Getter;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for Java AI applications.
 *
 * @author John Blum
 * @see java.lang.Runnable
 * @since 0.1.0
 */
public abstract class AbstractAiApplication implements Runnable {

	protected static final String AZURE_OPENAI_API_KEY_ENVIRONMENT_VARIABLE = "AZURE_OPENAI_KEY";
	protected static final String AZURE_OPENAI_SERVICE_URL_ENDPOINT = "https://jxblumspringazureopenai.openai.azure.com/";
	protected static final String AZURE_OPENAI_DEPLOYMENT = "AzureOpenAiGpt35TurboInstruct";
	protected static final String AZURE_OPENAI_MODEL = "gpt-35-turbo-instruct";
	protected static final String AZURE_OPENAI_DEPLOYMENT_MODEL = AZURE_OPENAI_DEPLOYMENT;
	protected static final String DATE_TIME_PATTERN = "yyyy-MMM-dd HH:mm:ss";

	protected static final Duration HTTP_CLIENT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

	protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

	protected static void log(String message, Object... arguments) {
		System.out.printf(message, arguments);
		System.out.flush();
	}

	protected void log(CompletionsUsage usage) {

		log("Prompt Tokens Used [%d]%n", usage.getPromptTokens());
		log("Completion Tokens Used [%d]%n", usage.getCompletionTokens());
		log("Total Tokens Used [%d]%n", usage.getTotalTokens());
	}

	protected void logCompletionsToJson(Completions completions) {

		try {
			log("JSON [%s]%n", newObjectMapper().writeValueAsString(completions));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to print Completions [%s] as JSON%n".formatted(completions), e);
		}
	}

	protected HttpClient newHttpClient() {

		HttpClientOptions httpClientOptions = new HttpClientOptions()
			.setConnectTimeout(HTTP_CLIENT_CONNECT_TIMEOUT);

		return HttpClient.createDefault(httpClientOptions);
	}

	protected ObjectMapper newObjectMapper() {

		return JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
			.enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
			.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.build()
			.findAndRegisterModules();
	}

	protected OpenAIClient newOpenAIClient(String azureOpenAiApiKey, String azureOpenAiServiceUrlEndpoint) {

		return new OpenAIClientBuilder()
			.httpClient(newHttpClient())
			.credential(new AzureKeyCredential(azureOpenAiApiKey))
			.endpoint(azureOpenAiServiceUrlEndpoint)
			.buildClient();
	}

	@Getter
	protected static class InterceptingHttpClient implements HttpClient {

		protected static @NotNull InterceptingHttpClient fromHttpClient(@NotNull HttpClient httpClient) {
			return new InterceptingHttpClient(httpClient);
		}

		private final HttpClient client;

		protected InterceptingHttpClient(@NotNull HttpClient client) {
			this.client = ObjectUtils.requireObject(client, "HttpClient is required");
		}

		@Override
		public Mono<HttpResponse> send(HttpRequest request) {

			return getClient().send(request)
				.doOnSuccess(httpResponse -> {

					int httpStatusCode = httpResponse.getStatusCode();

					log("HTTP status code [%d]%n", httpStatusCode);

					HttpHeaders httpHeaders = httpResponse.getHeaders();

					for (HttpHeader httpHeader : httpHeaders) {
						log("HTTP header [%s] is [%s]%n", httpHeader.getName(), httpHeader.getValue());
					}
				});
		}
	}
}
