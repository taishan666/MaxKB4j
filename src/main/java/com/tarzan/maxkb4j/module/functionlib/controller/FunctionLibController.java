package com.tarzan.maxkb4j.module.functionlib.controller;

import com.tarzan.maxkb4j.module.functionlib.service.FunctionLibService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@RestController
@AllArgsConstructor
public class FunctionLibController{

	private	final FunctionLibService functionLibService;
}
