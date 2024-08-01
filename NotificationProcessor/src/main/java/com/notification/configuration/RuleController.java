package com.notification.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.notification.dto.RuleDTO;
import com.notification.service.impl.RedisService;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

	@Autowired
	private RedisService redisService;

	@PostMapping("/add")
	public ResponseEntity<String> addRules(@RequestBody List<RuleDTO> ruleDTOList) {
		for (RuleDTO ruleDTO : ruleDTOList) {
			if (ruleDTO.getKey() == null || ruleDTO.getValue() == null) {
				return ResponseEntity.badRequest().body("Key and Value must not be null");
			}
			redisService.saveRule(ruleDTO.getKey(), ruleDTO.getValue());
		}
		return ResponseEntity.ok("Rules added successfully");
	}

}