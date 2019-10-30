package com.github.microprograms.micro_api_sdk.model;

import java.io.Serializable;

public class ErrorCodeDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 唯一标识符
	 */
	private String code;
	/**
	 * 错误提示信息
	 */
	private String message;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
