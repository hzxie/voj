package com.happystudio.voj.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -7683678924179048862L;

}
