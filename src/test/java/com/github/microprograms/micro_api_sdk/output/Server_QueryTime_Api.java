package com.github.microprograms.micro_api_sdk.output;

import com.github.microprograms.micro_entity_definition_runtime.annotation.Comment;
import com.github.microprograms.micro_api_runtime.annotation.MicroApiAnnotation;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Required;

@Comment(value = "查询服务器时间")
@MicroApiAnnotation(type = "read", version = "v1.0.3")
public class Server_QueryTime_Api {

    public static Response execute(Request request) throws Exception {
        Resp resp = new Resp();
        return resp;
    }

    public static class Resp extends Response {

        @Comment(value = "服务器时间戳")
        @Required(value = true)
        private Long timestamp;

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        @Comment(value = "服务器时间(格式yyyy-MM-dd HH:mm:ss)")
        @Required(value = true)
        private String time;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
