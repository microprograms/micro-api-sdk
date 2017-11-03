package com.github.microprograms.micro_api_sdk.output;

import com.github.microprograms.micro_api_sdk.TBrowseWebpageHistory;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Comment;
import com.github.microprograms.micro_api_runtime.annotation.MicroApiAnnotation;
import com.github.microprograms.micro_api_runtime.model.Response;
import com.github.microprograms.micro_api_runtime.model.Request;
import com.github.microprograms.micro_entity_definition_runtime.annotation.Required;

@Comment(value = "新增网页浏览历史记录")
@MicroApiAnnotation(type = "write", version = "v1.0.3")
public class TBrowseWebpageHistory_Create_Api {

    public static Response execute(Request request) throws Exception {
        Req req = (Req) request;
        Resp resp = new Resp();
        return resp;
    }

    public static class Req extends Request {

        @Comment(value = "网址")
        @Required(value = true)
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Comment(value = "标题")
        @Required(value = true)
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Comment(value = "主旨")
        @Required(value = false)
        private String gist;

        public String getGist() {
            return gist;
        }

        public void setGist(String gist) {
            this.gist = gist;
        }
    }

    public static class Resp extends Response {

        @Required(value = true)
        private TBrowseWebpageHistory object;

        public TBrowseWebpageHistory getObject() {
            return object;
        }

        public void setObject(TBrowseWebpageHistory object) {
            this.object = object;
        }
    }
}
