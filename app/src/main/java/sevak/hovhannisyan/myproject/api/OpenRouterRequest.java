package sevak.hovhannisyan.myproject.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenRouterRequest {
    @SerializedName("model")
    private String model;
    @SerializedName("messages")
    private List<Message> messages;

    public OpenRouterRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        @SerializedName("role")
        private String role;
        
        // This can be a String for text-only, or a List<Content> for vision
        @SerializedName("content")
        private Object content;

        public Message(String role, Object content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public Object getContent() { return content; }
    }

    public static class Content {
        @SerializedName("type")
        private String type;
        @SerializedName("text")
        private String text;
        @SerializedName("image_url")
        private ImageUrl imageUrl;

        public static Content text(String text) {
            Content c = new Content();
            c.type = "text";
            c.text = text;
            return c;
        }

        public static Content image(String base64) {
            Content c = new Content();
            c.type = "image_url";
            c.imageUrl = new ImageUrl("data:image/jpeg;base64," + base64);
            return c;
        }
    }

    public static class ImageUrl {
        @SerializedName("url")
        private String url;

        public ImageUrl(String url) {
            this.url = url;
        }
    }
}
