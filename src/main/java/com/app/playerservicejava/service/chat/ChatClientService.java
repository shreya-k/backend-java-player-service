package com.app.playerservicejava.service.chat;

import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.service.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.Model;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.types.OllamaModelType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@Service
public class ChatClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatClientService.class);

    @Autowired
    private OllamaAPI ollamaAPI;

    @Autowired
    private PlayerService playerService;

    public List<Model> listModels() throws OllamaBaseException, IOException, URISyntaxException, InterruptedException {
        List<Model> models = ollamaAPI.listModels();
        return models;
    }

    public String chat(String userInput) throws OllamaBaseException, IOException, InterruptedException {
        String model = OllamaModelType.TINYLLAMA;

        // take the userInput and build the initial prompt
        PromptBuilder promptBuilder = new PromptBuilder()
                .addLine("You are a JSON response generator.")
                .addLine("Given a user input, extract the player ID and the attribute mentioned.")
                .addLine("Return ONLY a valid JSON object with only playerid and attribute values")
                .addLine("Ensure the output is strictly formatted as JSON with no additional text, explanations, or code.")
                .addLine("Example user input: Can you give me the nickname for aaardsda01 based on his birth month?")
                .addLine("Expected response:")
                .addLine("{")
                .addLine("  \"playerId\": \"aaardsda01\",")
                .addLine("  \"attribute\": \"birthMonth\"")
                .addLine("}")
                .addLine("User Input: " + userInput);

        String firstP = promptBuilder.addLine("\nUser Input: " + userInput).build();

        boolean raw = false;
        OllamaResult response = ollamaAPI.generate(model, firstP, raw, new OptionsBuilder().build());
        String jsonResponse = response.getResponse();

        // Extract the JSON part from the response
        String jsonContent = jsonResponse.split("```json")[1].split("```")[0].trim();

        // Parse the JSON content
        JSONObject jsonObject = new JSONObject(jsonContent);

        String playerId = jsonObject.getString("playerId");
        String attribute = jsonObject.getString("attribute");

        LOGGER.info("Extracted playerId: {}, attribute: {}", playerId, attribute);

        // Fetch player details
        Optional<Player> playerOptional = playerService.getPlayerById(playerId);
        Player player = playerOptional.get();
        String name = player.getFirstName();
        String value = extractAttributeValue(player, attribute);

        // hard coding the values for testing purpose if null
        if(name == null|| name.isEmpty() ||  value == null || value.isEmpty()){
             name = "David";
             value = "October";
        }
        // Build final prompt for nickname generation
        PromptBuilder finalPromptBuilder = new PromptBuilder()
                .addLine("You are a creative nickname generator.")
                .addLine("Generate a unique one word nickname for the person named:")
                .addLine("Consider their name and any additional information to make the nickname memorable, and personalized.")
                .addLine("Only provide the nickname in your response, without any extra text.")
                .addLine("Name: " + name)
                .addLine("Additional Information: " + value)
                .addLine("Response:")
                .addLine("Nickname:");
        OllamaResult finalResponse = ollamaAPI.generate(model, finalPromptBuilder.build(), raw, new OptionsBuilder().build());
        return finalResponse.getResponse(); // tinyllama response

    }

    //to parse json
    private String extractAttributeValue(Player player, String attribute) {
        switch (attribute.toLowerCase()) {
            case "birthmonth":
                return player.getBirthMonth();
            case "lastname":
                return player.getLastName();
            case "birthcity":
                return player.getBirthCity();
            // Add other attributes as needed
            default:
                return null;
        }
    }

}




/*
Note : things to improve/add
1.add logging and exception handling
2. fine tune both the prompts
3.integrate with player service model/ or train the model on player.csv
*/
