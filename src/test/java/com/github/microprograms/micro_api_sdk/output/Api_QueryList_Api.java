package com.github.microprograms.micro_api_sdk.output;

import java.util.List;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Comment;
import com.github.microprograms.micro_api_runtime.annotation.MicroApiAnnotation;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Required;

@Comment(value = "查询API列表")
@MicroApiAnnotation(type = "read", version = "v1.0.3")
public class Api_QueryList_Api {

    public static Response execute(Request request) throws Exception {
        Resp resp = new Resp();
        return resp;
    }

    public static class Resp extends Response {

        @Comment(value = "API接口名字列表")
        @Required(value = true)
        private List<String> apiList;

        public List<String> getApiList() {
            return apiList;
        }

        public void setApiList(List<String> apiList) {
            this.apiList = apiList;
        }
    }
}
