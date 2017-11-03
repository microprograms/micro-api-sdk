package com.github.microprograms.micro_api_sdk.output;

import java.util.List;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Comment;
import com.github.microprograms.micro_api_runtime.annotation.MicroApiAnnotation;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Required;

@Comment(value = "查询我的阅读历史记录")
@MicroApiAnnotation(type = "read", version = "v1.0.3")
public class MyBooks_Query_Api {

    public static Response execute(Request request) throws Exception {
        Resp resp = new Resp();
        return resp;
    }

    public static class Resp extends Response {

        @Required(value = true)
        private List<Book> list;

        public List<Book> getList() {
            return list;
        }

        public void setList(List<Book> list) {
            this.list = list;
        }
    }
}
