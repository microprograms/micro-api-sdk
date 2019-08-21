package com.github.microprograms.micro_api_sdk.model;

import java.util.List;

public class ChangeLog {
	private String version;
	private List<String> items;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getItems() {
		return items;
	}

	public void setItems(List<String> items) {
		this.items = items;
	}
}
