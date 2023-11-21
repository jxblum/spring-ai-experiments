package io.vmware.spring.ai.azure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.Choice;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.ai.openai.models.CompletionsUsage;

/**
 * Example Java application to test Microsoft Azure OpenAI Service, Chat Completions service endpoint.
 *
 * @author John Blum
 * @see io.vmware.spring.ai.azure.openai.AbstractAiApplication
 * @see com.azure.ai.openai.OpenAIClient
 * @since 0.1.0
 */
public class AzureOpenAiApplication extends AbstractAiApplication {

	private static final int AI_MAX_COMPLETION_CHOICES = 2;
	private static final int AI_MAX_TOKENS_PER_REQUEST = 1000;

	private static final double AI_TEMPERATURE = 0.75d;

	public static void main(String[] args) {
		new AzureOpenAiApplication().run();
	}

	@Override
	public void run() {

		OpenAIClient client = newOpenAIClient();

		CompletionsOptions completionsOptions = newCompletionsOptions("Give me 2 Java learning references.",
			"Give me 2 Kotlin learning references.");

		Completions completions = client.getCompletions(AZURE_OPENAI_DEPLOYMENT_MODEL, completionsOptions);

		log("Completions ID [%s] completed at [%s]%n", completions.getId(),
			completions.getCreatedAt().format(DATE_TIME_FORMATTER));

		assertThat(completions).isNotNull();

		logCompletionsToJson(completions);

		CompletionsUsage usage = completions.getUsage();

		log(usage);

		for (Choice choice : completions.getChoices()) {
			log("Choice Index [%d] & Message [%s]%n", choice.getIndex(), choice.getText());
		}
	}

	private OpenAIClient newOpenAIClient() {
		String azureOpenAiApiKey = System.getenv(AZURE_OPENAI_API_KEY_ENVIRONMENT_VARIABLE);
		return newOpenAIClient(azureOpenAiApiKey, AZURE_OPENAI_SERVICE_URL_ENDPOINT);
	}

	@Override
	protected InterceptingHttpClient newHttpClient() {
		return InterceptingHttpClient.fromHttpClient(super.newHttpClient());
	}

	private CompletionsOptions newCompletionsOptions(String... prompt) {
		return newCompletionsOptions(Arrays.asList(prompt));
	}

	private CompletionsOptions newCompletionsOptions(List<String> prompt) {

		return new CompletionsOptions(prompt)
			.setMaxTokens(AI_MAX_TOKENS_PER_REQUEST)
			.setModel(AZURE_OPENAI_MODEL)
			.setN(AI_MAX_COMPLETION_CHOICES)
			.setTemperature(AI_TEMPERATURE);
	}
}
