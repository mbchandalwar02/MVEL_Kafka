package com.notification.service.impl;

import java.sql.Array;
import java.util.*;

import org.json.JSONObject;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class RedisService {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	public void saveRule(String key, String value) {
		redisTemplate.opsForValue().set(key, value);
	}

	public JSONObject evaluateAndUpdate(JSONObject payload) {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> map = null;
		List<String> keys = new ArrayList<>();
		List<String> steps = new ArrayList<>();

		try {
			// Convert JSONObject to a map
			map = objectMapper.readValue(payload.toString(), HashMap.class);

			// Get rules and steps lists from the map
			keys = (List<String>) map.get("rules");
			steps = (List<String>) map.get("steps");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		// Prepare rules map
		Map<String, String> rules = new HashMap<>();
		if (keys != null) {
			for (String key : keys) {
				String rule = (String) redisTemplate.opsForValue().get(key);
				if (rule != null) {
					rules.put(key, rule);
				}
			}
		}

		// Create response object and remove keys
		if (map != null) {
			map.remove("rules");
			map.remove("steps");
		}
		JSONObject response = new JSONObject(map);

		// Execute pipeline steps
		for (String step : steps) {
			String validationFailure = executePipelineSteps(step, rules, map);
			if (!"success".equals(validationFailure)) {
				response.put("status", "failure");
				response.put("message", validationFailure);
				return response;
			}

			response = new JSONObject(map);
		}

		// If no failure, mark as success
		response.put("status", "success");
		response.put("message", "All steps completed successfully");

		return response;
	}


	private String executePipelineSteps(String step, Map<String, String> rules, Map<String, Object> payload) {
		// Use Streams to filter and iterate over rules that contain the specified step
		for (Map.Entry<String, String> entry : rules.entrySet().stream()
				.filter(e -> e.getKey().contains(step))
				.collect(Collectors.toList())) {
			try {
				Object result = MVEL.eval(entry.getValue(), payload);
				System.out.println(payload);
				if (result == null || (result instanceof Boolean && !(Boolean) result)) {
					String failureMessage = "Failure in step: " + step + " - Rule: " + entry.getKey();
					// logger.error(failureMessage);
					return failureMessage;
				}
			} catch (Exception e) {
				String failureMessage = "Exception in step: " + step + " - Rule: " + entry.getKey() + " - " + e.getMessage();
				// logger.error(failureMessage, e);
				return failureMessage;
			}
		}
		return "success";
	}
}