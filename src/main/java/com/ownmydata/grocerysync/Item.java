package com.ownmydata.grocerysync;

import com.android.data.Document;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public class Item extends Document {
    public static final String USERS = "users";
    private Date createdAt;
    private String text;
    private boolean checked;

    @JsonCreator
    public Item(@JsonProperty("text") String text,
                @JsonProperty("checked") boolean checked,
                @JsonProperty("createdAt") Date createdAt) {
        this.createdAt = createdAt;
        this.text = text;
        this.checked = checked;
        shareWith(USERS);
    }

    public Item(String text, boolean checked) {
        this(text, checked, new Date());
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getText() {
        return text;
    }

    public boolean isChecked() {
        return checked;
    }

    public void toggleCheck() {
        checked = !checked;
    }
}
