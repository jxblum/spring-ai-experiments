package io.vmware.spring.ai.azure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.Choice;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.ai.openai.models.CompletionsUsage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.HttpClientOptions;

import org.cp.elements.lang.ObjectUtils;
import org.cp.elements.lang.annotation.NotNull;

import lombok.Getter;
import reactor.core.publisher.Mono;

/**
 * Example Java application to test Microsoft Azure OpenAI Service, Chat Completions service endpoint.
 *
 * @author John Blum
 * @since 0.1.0
 */
public class ExampleAiApplication implements Runnable {

	private static final int AI_MAX_TOKENS_PER_REQUEST = 1000;

	private static final double AI_TEMPERATURE = 0.75d;

	private static final Duration HTTP_CLIENT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final String AZURE_OPENAI_API_KEY_ENVIRONMENT_VARIABLE = "AZURE_OPENAI_KEY";
	private static final String AZURE_OPENAI_SERVICE_URL_ENDPOINT = "https://jxblumspringazureopenai.openai.azure.com/";
	private static final String AZURE_OPENAI_DEPLOYMENT = "AzureOpenAiGpt35TurboInstruct";
	private static final String AZURE_OPENAI_MODEL = "gpt-35-turbo-instruct";
	private static final String AZURE_OPENAI_DEPLOYMENT_MODEL = AZURE_OPENAI_DEPLOYMENT;
	private static final String DATE_TIME_PATTERN = "yyyy-MMM-dd HH:mm:ss";

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

	public static void main(String[] args) {
		new ExampleAiApplication().run();
	}

	private static void log(String message, Object... arguments) {
		System.out.printf(message, arguments);
		System.out.flush();
	}

	@Override
	public void run() {

		OpenAIClient client = newOpenAIClient();

		CompletionsOptions completionsOptions = newCompletionsOptions("When was Apple founded?");

		Completions completions = client.getCompletions(AZURE_OPENAI_DEPLOYMENT_MODEL, completionsOptions);

		log("Completions ID [%s] completed at [%s]%n", completions.getId(),
			completions.getCreatedAt().format(DATE_TIME_FORMATTER));

		assertThat(completions).isNotNull();

		CompletionsUsage usage = completions.getUsage();

		log(usage);

		for (Choice choice : completions.getChoices()) {
			log("Choice Index [%d] & Message [%s]%n", choice.getIndex(), choice.getText());
		}
	}

	private OpenAIClient newOpenAIClient() {
		String azureOpenAiApiKey = System.getenv(AZURE_OPENAI_API_KEY_ENVIRONMENT_VARIABLE);
		return newOpenAIClient(azureOpenAiApiKey);
	}

	private OpenAIClient newOpenAIClient(String azureOpenAiApiKey) {

		return new OpenAIClientBuilder()
			.httpClient(newHtptClient())
			.credential(new AzureKeyCredential(azureOpenAiApiKey))
			.endpoint(AZURE_OPENAI_SERVICE_URL_ENDPOINT)
			.buildClient();
	}

	private InterceptingHttpClient newHtptClient() {

		HttpClientOptions httpClientOptions = new HttpClientOptions()
			.setConnectTimeout(HTTP_CLIENT_CONNECT_TIMEOUT);

		return InterceptingHttpClient.fromHttpClient(HttpClient.createDefault(httpClientOptions));
	}

	private CompletionsOptions newCompletionsOptions(String... prompt) {
		return newCompletionsOptions(Arrays.asList(prompt));
	}

	private CompletionsOptions newCompletionsOptions(List<String> prompt) {

		return new CompletionsOptions(prompt)
			.setMaxTokens(AI_MAX_TOKENS_PER_REQUEST)
			.setModel(AZURE_OPENAI_MODEL)
			.setTemperature(AI_TEMPERATURE);
	}

	private void log(CompletionsUsage usage) {
		log("Prompt Tokens Used [%d]%n", usage.getPromptTokens());
		log("Completion Tokens Used [%d]%n", usage.getCompletionTokens());
		log("Total Tokens Used [%d]%n", usage.getTotalTokens());
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
